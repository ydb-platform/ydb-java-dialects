package tech.ydb.trino;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import io.opentelemetry.api.internal.StringUtils;
import io.trino.plugin.base.aggregation.AggregateFunctionRewriter;
import io.trino.plugin.base.aggregation.AggregateFunctionRule;
import io.trino.plugin.base.expression.ConnectorExpressionRewriter;
import io.trino.plugin.base.mapping.IdentifierMapping;
import io.trino.plugin.base.projection.ProjectFunctionRewriter;
import io.trino.plugin.base.projection.ProjectFunctionRule;
import io.trino.plugin.jdbc.*;
import io.trino.plugin.jdbc.aggregation.ImplementAvgDecimal;
import io.trino.plugin.jdbc.aggregation.ImplementAvgFloatingPoint;
import io.trino.plugin.jdbc.aggregation.ImplementCount;
import io.trino.plugin.jdbc.aggregation.ImplementCountAll;
import io.trino.plugin.jdbc.aggregation.ImplementCountDistinct;
import io.trino.plugin.jdbc.aggregation.ImplementMinMax;
import io.trino.plugin.jdbc.aggregation.ImplementSum;
import io.trino.plugin.jdbc.expression.JdbcConnectorExpressionRewriterBuilder;
import io.trino.plugin.jdbc.expression.ParameterizedExpression;
import io.trino.plugin.jdbc.expression.RewriteIn;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.connector.*;
import io.trino.spi.expression.ConnectorExpression;
import io.trino.spi.type.*;
import io.trino.spi.TrinoException;

import jakarta.annotation.Nullable;
import org.jspecify.annotations.NonNull;

import java.sql.*;
import java.util.List;
import java.util.*;
import java.util.stream.Stream;

import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;
import static io.trino.plugin.jdbc.PredicatePushdownController.DISABLE_PUSHDOWN;
import static io.trino.plugin.jdbc.StandardColumnMappings.*;
import static io.trino.spi.StandardErrorCode.NOT_SUPPORTED;
import static io.trino.spi.connector.ConnectorMetadata.MODIFYING_ROWS_MESSAGE;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.BooleanType.BOOLEAN;
import static io.trino.spi.type.DateType.DATE;
import static io.trino.spi.type.DecimalType.createDecimalType;
import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.spi.type.RealType.REAL;
import static io.trino.spi.type.SmallintType.SMALLINT;
import static io.trino.spi.type.TimestampType.TIMESTAMP_MICROS;
import static io.trino.spi.type.TinyintType.TINYINT;
import static io.trino.spi.type.VarcharType.createUnboundedVarcharType;
import static io.trino.spi.type.VarcharType.createVarcharType;
import static java.lang.Math.max;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class YdbClient extends BaseJdbcClient {
    private static final String YDB_SCHEMA = "ydb";

    private final ConnectorExpressionRewriter<ParameterizedExpression> connectorExpressionRewriter;
    private final AggregateFunctionRewriter<JdbcExpression, ParameterizedExpression> aggregateFunctionRewriter;
    private final ProjectFunctionRewriter<JdbcExpression, ParameterizedExpression> projectFunctionRewriter;

    @Inject
    public YdbClient(
            BaseJdbcConfig config,
            ConnectionFactory connectionFactory,
            QueryBuilder queryBuilder,
            IdentifierMapping identifierMapping,
            RemoteQueryModifier remoteQueryModifier
    ) {
        super(
                "`",
                connectionFactory,
                queryBuilder,
                config.getJdbcTypesMappedToVarchar(),
                identifierMapping,
                remoteQueryModifier,
                true
        );

        this.connectorExpressionRewriter = JdbcConnectorExpressionRewriterBuilder.newBuilder()
                .addStandardRules(this::quoted)
                .add(new RewriteIn())
                .add(new RewriteDivideModulus())
                .add(new RewriteNullIf())
                .withTypeClass("integer_type", ImmutableSet.of("tinyint", "smallint", "integer", "bigint"))
                .withTypeClass("numeric_type", ImmutableSet.of("tinyint", "smallint", "integer", "bigint", "decimal", "real", "double"))
                .map("$equal(left, right)").to("left = right")
                .map("$not_equal(left, right)").to("left <> right")
                .map("$add(left: integer_type, right: integer_type)").to("left + right")
                .map("$subtract(left: integer_type, right: integer_type)").to("left - right")
                .map("$multiply(left: integer_type, right: integer_type)").to("left * right")
                .map("$negate(value: integer_type)").to("-value")
                // TODO in some cases we can actually push down comparison on varchar
                .map("$less_than(left: numeric_type, right: numeric_type)").to("left < right")
                .map("$less_than_or_equal(left: numeric_type, right: numeric_type)").to("left <= right")
                .map("$greater_than(left: numeric_type, right: numeric_type)").to("left > right")
                .map("$greater_than_or_equal(left: numeric_type, right: numeric_type)").to("left >= right")
                .map("$is_null(value)").to("value IS NULL")
                .map("$not($is_null(value))").to("value IS NOT NULL")
                .map("$concat(left: varchar, right: varchar)").to("left || right")
                .build();

        this.projectFunctionRewriter = new ProjectFunctionRewriter<>(
                this.connectorExpressionRewriter,
                ImmutableSet.<ProjectFunctionRule<JdbcExpression, ParameterizedExpression>>builder()
                        .add(new RewriteUnaryStringOperations())
                        .add(new RewriteStringPosition())
                        .build());

        JdbcTypeHandle bigintTypeHandle = YdbTypeUtils.toTypeHandle(BIGINT).orElseThrow();
        this.aggregateFunctionRewriter = new AggregateFunctionRewriter<>(
                this.connectorExpressionRewriter,
                ImmutableSet.<AggregateFunctionRule<JdbcExpression, ParameterizedExpression>>builder()
                        .add(new ImplementCountAll(bigintTypeHandle))
                        .add(new ImplementMinMax(true))
                        .add(new ImplementCount(bigintTypeHandle))
                        .add(new ImplementCountDistinct(bigintTypeHandle, true))
                        .add(new ImplementSum(YdbTypeUtils::toTypeHandle))
                        .add(new ImplementAvgFloatingPoint())
                        .add(new ImplementAvgDecimal())
                        .build());
    }

    @Override
    public Optional<JdbcExpression> implementAggregation(
            ConnectorSession session,
            AggregateFunction aggregate,
            Map<String, ColumnHandle> assignments
    ) {
        return aggregateFunctionRewriter.rewrite(session, aggregate, assignments);
    }

    @Override
    public Optional<ParameterizedExpression> convertPredicate(
            ConnectorSession session,
            ConnectorExpression expression,
            Map<String, ColumnHandle> assignments
    ) {
        return connectorExpressionRewriter.rewrite(session, expression, assignments);
    }

    @Override
    public Optional<JdbcExpression> convertProjection(
            ConnectorSession session,
            JdbcTableHandle handle,
            ConnectorExpression expression,
            Map<String, ColumnHandle> assignments
    ) {
        JdbcTypeHandle typeHandle = YdbTypeUtils.toTypeHandle(expression.getType()).orElse(null);
        if (Objects.isNull(typeHandle)) {
            return Optional.empty();
        }
        Optional<ParameterizedExpression> result = connectorExpressionRewriter.rewrite(session, expression, assignments);
        return result.map(parameterizedExpression -> new JdbcExpression(
                parameterizedExpression.expression(),
                parameterizedExpression.parameters(),
                typeHandle)).or(() -> projectFunctionRewriter.rewrite(session, handle, expression, assignments));
    }

    @Override
    public Collection<String> listSchemas(Connection connection) {
        return ImmutableSet.of(YDB_SCHEMA);
    }

    @Override
    protected String escapeObjectNameForMetadataQuery(String name, String escape) {
        return name;
    }

    @Override
    public List<SchemaTableName> getTableNames(ConnectorSession session, Optional<String> schema) {
        try (Connection connection = connectionFactory.openConnection(session)) {
            try (ResultSet resultSet = getTables(connection, Optional.empty(), Optional.empty())) {
                ImmutableList.Builder<@NonNull SchemaTableName> list = ImmutableList.builder();
                while (resultSet.next()) {
                    String tableName = resultSet.getString("TABLE_NAME");
                    list.add(new SchemaTableName(YDB_SCHEMA, tableName));
                }
                return list.build();
            }
        } catch (SQLException e) {
            throw new TrinoException(JDBC_ERROR, e);
        }
    }

    @Override
    public Optional<JdbcTableHandle> getTableHandle(
            ConnectorSession session,
            SchemaTableName schemaTableName
    ) {
        try (Connection connection = connectionFactory.openConnection(session)) {
            RemoteTableName remoteTableName = toRemoteTableName(schemaTableName);
            try (ResultSet columns = getColumns(remoteTableName, connection.getMetaData())) {
                if (!columns.next()) {
                    return Optional.empty();
                }
            }
            return Optional.of(new JdbcTableHandle(
                    new SchemaTableName(YDB_SCHEMA, schemaTableName.getTableName()),
                    remoteTableName,
                    Optional.empty())
            );
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ColumnMapping> toColumnMapping(
            ConnectorSession session,
            Connection connection,
            JdbcTypeHandle typeHandle
    ) {
        Optional<ColumnMapping> mapping = getForcedMappingToVarchar(typeHandle);
        if (mapping.isPresent()) {
            return mapping;
        }

        Optional<ColumnMapping> columnMapping = switch (typeHandle.jdbcType()) {
            case Types.BIT, Types.BOOLEAN -> Optional.of(booleanColumnMapping());
            case Types.TINYINT, Types.SMALLINT -> Optional.of(smallintColumnMapping());
            case Types.INTEGER -> Optional.of(integerColumnMapping());
            case Types.BIGINT -> Optional.of(bigintColumnMapping());
            case Types.REAL -> Optional.of(realColumnMapping());
            case Types.FLOAT, Types.DOUBLE -> Optional.of(doubleColumnMapping());
            case Types.DECIMAL -> {
                // We need this hack because JDBC client for some reason does not return the scale
                // in .requiredDecimalDigits() and we have to parse it manually.
                String typeName = typeHandle.jdbcTypeName().get();
                int start = typeName.indexOf('(');
                int end = typeName.indexOf(')');
                String[] parts = typeName.substring(start + 1, end).split(",");
                int precision = Integer.parseInt(parts[0].trim());
                int scale = Integer.parseInt(parts[1].trim());

                yield Optional.of(ColumnMapping.mapping(
                        createDecimalType(precision, max(scale, 0)),
                        decimalColumnMapping(createDecimalType(precision, max(scale, 0))).getReadFunction(),
                        decimalColumnMapping(createDecimalType(precision, max(scale, 0))).getWriteFunction(),
                        DISABLE_PUSHDOWN));
            }
            case Types.CHAR, Types.NCHAR -> {
                String typeName = typeHandle.jdbcTypeName().orElseThrow();
                int length = typeName.toLowerCase().startsWith("char(")
                        ? Integer.parseInt(typeName.substring(5, typeName.length() - 1))
                        : typeHandle.columnSize().orElse(VarcharType.MAX_LENGTH);
                yield Optional.of(varcharColumnMapping(length));
            }
            case Types.VARCHAR, Types.LONGVARCHAR, Types.NVARCHAR -> {
                String typeName = typeHandle.jdbcTypeName().orElseThrow();
                int length = typeName.toLowerCase().startsWith("varchar(")
                        ? Integer.parseInt(typeName.substring(8, typeName.length() - 1))
                        : typeHandle.columnSize().orElse(VarcharType.MAX_LENGTH);
                yield Optional.of(varcharColumnMapping(length));
            }
            case Types.DATE -> Optional.of(dateColumnMapping());
            case Types.TIMESTAMP -> Optional.of(timestampColumnMapping());
            default -> Optional.empty();
        };

        if (columnMapping.isPresent()) {
            return columnMapping;
        }

        return mapToUnboundedVarchar(typeHandle);
    }

    private static ColumnMapping varcharColumnMapping(int varcharLength) {
        VarcharType varcharType = varcharLength <= VarcharType.MAX_LENGTH
                ? createVarcharType(varcharLength)
                : createUnboundedVarcharType();
        return ColumnMapping.sliceMapping(
                varcharType,
                varcharReadFunction(varcharType),
                varcharWriteFunction(),
                DISABLE_PUSHDOWN);
    }

    private static ColumnMapping dateColumnMapping() {
        return ColumnMapping.longMapping(
                DATE,
                dateReadFunctionUsingLocalDate(),
                dateWriteFunctionUsingLocalDate());
    }

    private static ColumnMapping timestampColumnMapping() {
        return ColumnMapping.longMapping(
                TIMESTAMP_MICROS,
                timestampReadFunction(TIMESTAMP_MICROS),
                timestampWriteFunction(TIMESTAMP_MICROS));
    }

    @Override
    public WriteMapping toWriteMapping(ConnectorSession session, Type type) {
        if (type == BOOLEAN) {
            return WriteMapping.booleanMapping("Bool", BooleanWriteFunction.of(Types.BOOLEAN, PreparedStatement::setBoolean));
        }
        if (type == TINYINT) {
            return WriteMapping.longMapping("Int8", tinyintWriteFunction());
        }
        if (type == SMALLINT) {
            return WriteMapping.longMapping("Int16", smallintWriteFunction());
        }
        if (type == INTEGER) {
            return WriteMapping.longMapping("Int32", integerWriteFunction());
        }
        if (type == BIGINT) {
            return WriteMapping.longMapping("Int64", bigintWriteFunction());
        }
        if (type == REAL) {
            return WriteMapping.longMapping("Float", realWriteFunction());
        }
        if (type == DOUBLE) {
            return WriteMapping.doubleMapping("Double", doubleWriteFunction());
        }
        if (type instanceof DecimalType decimalType) {
            String dataType = format("Decimal(%s, %s)", decimalType.getPrecision(), decimalType.getScale());
            return decimalType.isShort()
                    ? WriteMapping.longMapping(dataType, shortDecimalWriteFunction(decimalType))
                    : WriteMapping.objectMapping(dataType, longDecimalWriteFunction(decimalType));
        }
        if (type instanceof VarcharType) {
            return WriteMapping.sliceMapping("String", varcharWriteFunction());
        }
        if (type instanceof CharType) {
            return WriteMapping.sliceMapping("String", charWriteFunction());
        }
        if (type == DATE) {
            return WriteMapping.longMapping("Date", dateWriteFunctionUsingLocalDate());
        }
        if (type == TIMESTAMP_MICROS) {
            return WriteMapping.longMapping("Timestamp", timestampWriteFunction(TIMESTAMP_MICROS));
        }

        throw new TrinoException(NOT_SUPPORTED, "Unsupported column type: " + type);
    }

    @Override
    public boolean supportsTopN(ConnectorSession session, JdbcTableHandle handle, List<JdbcSortItem> sortOrder) {
        return true;
    }

    @Override
    @SuppressWarnings("all")
    protected Optional<TopNFunction> topNFunction() {
        // We need the hack below because Trino and YDB handle nulls differently when sorting.
        return Optional.of((query, sortItems, limit) -> {
            String orderBy = sortItems.stream()
                    .flatMap(sortItem -> {
                        String columnName = quoted(sortItem.column().getColumnName());
                        SortOrder sortOrder = sortItem.sortOrder();
                        // NULLS FIRST:  CASE WHEN col IS NULL THEN 0 ELSE 1 END ASC
                        // NULLS LAST:   CASE WHEN col IS NULL THEN 1 ELSE 0 END ASC
                        if (sortOrder.isNullsFirst()) {
                            // Add null-sorting key first, then the actual column
                            String nullSort = format("CASE WHEN %s IS NULL THEN 0 ELSE 1 END ASC", columnName);
                            String valueSort = format("%s %s", columnName, sortOrder.isAscending() ? "ASC" : "DESC");
                            return Stream.of(nullSort, valueSort);
                        } else {
                            // NULLS LAST
                            String nullSort = format("CASE WHEN %s IS NULL THEN 1 ELSE 0 END ASC", columnName);
                            String valueSort = format("%s %s", columnName, sortOrder.isAscending() ? "ASC" : "DESC");
                            return Stream.of(nullSort, valueSort);
                        }
                    })
                    .collect(joining(", "));
            return format("%s ORDER BY %s LIMIT %d", query, orderBy, limit);
        });
    }

    @Override
    public boolean isTopNGuaranteed(ConnectorSession session) {
        return true;
    }

    private RemoteTableName toRemoteTableName(SchemaTableName schemaTableName) {
        return new RemoteTableName(Optional.empty(), Optional.empty(), schemaTableName.getTableName());
    }

    @Override
    protected String quoted(@Nullable String catalog, @Nullable String schema, String table) {
        // YDB doesn't use catalog & schema in table names, only the table path
        return quoted(table);
    }

    @Override
    protected void execute(ConnectorSession session, Connection connection, String query) throws SQLException {
        // TODO this connector does not actually support delete yet, I can't include delete tests without update tests
        if (query.startsWith("TRUNCATE TABLE")) {
            query = query.replace("TRUNCATE TABLE", "DELETE FROM");
        }

        final String finalQuery = query;
        YdbRetryUtils.withRetry(() -> super.execute(session, connection, finalQuery));
    }

    @Override
    public boolean supportsRetries() {
        // Disable Trino-retries to avoid temporary tables.
        return false;
    }

    @Override
    protected String getColumnDefinitionSql(ConnectorSession session, ColumnMetadata column, String columnName) {
        // YDB restriction, does not support column comments.
        if (!StringUtils.isNullOrEmpty(column.getComment())) {
            throw new TrinoException(NOT_SUPPORTED, "This connector does not support creating tables with column comment");
        }

        StringBuilder sb = new StringBuilder()
                .append(quoted(columnName))
                .append(" ")
                .append(toWriteMapping(session, column.getType()).getDataType());

        if (!column.isNullable()) {
            sb.append(" NOT NULL");
        }
        if (column.getDefaultValue().isPresent()) {
            sb.append(" DEFAULT ").append(column.getDefaultValue().get());
        }

        return sb.toString();
    }

    @Override
    protected void addColumn(
            ConnectorSession session,
            Connection connection,
            RemoteTableName table,
            ColumnMetadata column
    ) throws SQLException {
        String columnName = column.getName();
        String remoteColumnName = getIdentifierMapping().toRemoteColumnName(getRemoteIdentifiers(connection), columnName);
        String sql = format(
                "ALTER TABLE %s ADD %s",
                quoted(table),
                getColumnDefinitionSql(session, column, remoteColumnName));
        execute(session, connection, sql);
    }

    @Override
    protected void renameColumn(
            ConnectorSession session,
            Connection connection,
            RemoteTableName remoteTableName,
            String remoteColumnName,
            String newRemoteColumnName
    ) throws SQLException {
        execute(session, connection, format(
                "ALTER TABLE %s RENAME COLUMN %s TO %s",
                quoted(remoteTableName),
                quoted(remoteColumnName),
                quoted(newRemoteColumnName))
        );
    }

    // TODO maybe support in the future :)
    @Override
    public JdbcMergeTableHandle beginMerge(
            ConnectorSession session,
            JdbcTableHandle handle,
            Map<Integer, Collection<ColumnHandle>> updateColumnHandles,
            List<Runnable> rollbackActions,
            RetryMode retryMode
    ) {
        throw new TrinoException(NOT_SUPPORTED, MODIFYING_ROWS_MESSAGE);
    }

    // TODO maybe support in the future :)
    @Override
    public OptionalLong delete(
            ConnectorSession session,
            JdbcTableHandle handle
    ) {
        throw new TrinoException(NOT_SUPPORTED, MODIFYING_ROWS_MESSAGE);
    }

    @Override
    protected List<String> createTableSqls(RemoteTableName remoteTableName, List<String> columns, ConnectorTableMetadata tableMetadata) {
        return super.createTableSqls(remoteTableName, columns, tableMetadata);
    }
}