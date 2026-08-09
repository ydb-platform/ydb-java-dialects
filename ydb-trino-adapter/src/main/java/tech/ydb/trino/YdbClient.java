package tech.ydb.trino;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;
import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;
import static io.trino.plugin.jdbc.PredicatePushdownController.DISABLE_PUSHDOWN;
import static io.trino.plugin.jdbc.PredicatePushdownController.FULL_PUSHDOWN;
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
                .withTypeClass("comparable_type", ImmutableSet.of(
                        "tinyint", "smallint", "integer", "bigint", "decimal", "real", "double", "varchar", "char", "date", "timestamp"))
                .map("$equal(left, right)").to("left = right")
                .map("$not_equal(left, right)").to("left <> right")
                .map("$add(left: integer_type, right: integer_type)").to("left + right")
                .map("$subtract(left: integer_type, right: integer_type)").to("left - right")
                .map("$multiply(left: integer_type, right: integer_type)").to("left * right")
                .map("$negate(value: integer_type)").to("-value")
                .map("$less_than(left: comparable_type, right: comparable_type)").to("left < right")
                .map("$less_than_or_equal(left: comparable_type, right: comparable_type)").to("left <= right")
                .map("$greater_than(left: comparable_type, right: comparable_type)").to("left > right")
                .map("$greater_than_or_equal(left: comparable_type, right: comparable_type)").to("left >= right")
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
    @SuppressWarnings("all")
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

        // YDB JDBC reports text columns as Bytes/String/Utf8/Text under various JDBC type codes.
        // Always map them to unbounded varchar (YDB Text has no useful fixed length).
        String jdbcTypeName = typeHandle.jdbcTypeName().orElse("").toLowerCase(Locale.ROOT);
        if (jdbcTypeName.equals("bytes")
                || jdbcTypeName.equals("string")
                || jdbcTypeName.equals("utf8")
                || jdbcTypeName.equals("text")) {
            return Optional.of(unboundedVarcharColumnMapping());
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

    private static ColumnMapping unboundedVarcharColumnMapping() {
        VarcharType varcharType = createUnboundedVarcharType();
        return ColumnMapping.sliceMapping(
                varcharType,
                varcharReadFunction(varcharType),
                varcharWriteFunction(),
                FULL_PUSHDOWN);
    }

    private static ColumnMapping varcharColumnMapping(int varcharLength) {
        VarcharType varcharType = varcharLength <= VarcharType.MAX_LENGTH
                ? createVarcharType(varcharLength)
                : createUnboundedVarcharType();
        return ColumnMapping.sliceMapping(
                varcharType,
                varcharReadFunction(varcharType),
                varcharWriteFunction(),
                FULL_PUSHDOWN);
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
            return WriteMapping.sliceMapping("Text", varcharWriteFunction());
        }
        if (type instanceof CharType) {
            return WriteMapping.sliceMapping("Text", charWriteFunction());
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

    @Override
    protected Optional<BiFunction<String, Long, String>> limitFunction() {
        return Optional.of((sql, limit) -> sql + " LIMIT " + limit);
    }

    @Override
    public boolean isLimitGuaranteed(ConnectorSession session) {
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
        YdbRetryUtils.withRetry(() -> super.execute(session, connection, query));
    }

    @Override
    public boolean supportsRetries() {
        // Disable Trino-retries to avoid temporary tables.
        return false;
    }

    @Override
    public void createSchema(ConnectorSession session, String schemaName) {
        throw new TrinoException(NOT_SUPPORTED, "This connector does not support creating schemas");
    }

    @Override
    public void dropSchema(ConnectorSession session, String schemaName, boolean cascade) {
        throw new TrinoException(NOT_SUPPORTED, "This connector does not support dropping schemas");
    }

    @Override
    public void renameSchema(ConnectorSession session, String schemaName, String newSchemaName) {
        throw new TrinoException(NOT_SUPPORTED, "This connector does not support renaming schemas");
    }

    @Override
    public void dropColumn(ConnectorSession session, JdbcTableHandle handle, JdbcColumnHandle column) {
        try (Connection connection = connectionFactory.openConnection(session)) {
            String sql = format(
                    "ALTER TABLE %s DROP COLUMN %s",
                    quoted(handle.asPlainTable().getRemoteTableName().getTableName()),
                    quoted(column.getColumnMetadata().getName()));
            execute(session, connection, sql);
        } catch (SQLException e) {
            throw new TrinoException(JDBC_ERROR, e);
        }
    }

    @Override
    public void setColumnType(ConnectorSession session, JdbcTableHandle handle, JdbcColumnHandle column, Type type) {
        throw new TrinoException(NOT_SUPPORTED, "This connector does not support setting column types");
    }

    @Override
    public void dropNotNullConstraint(ConnectorSession session, JdbcTableHandle handle, JdbcColumnHandle column) {
        try (Connection connection = connectionFactory.openConnection(session)) {
            String sql = format(
                    "ALTER TABLE %s ALTER COLUMN %s DROP NOT NULL",
                    quoted(handle.asPlainTable().getRemoteTableName().getTableName()),
                    quoted(column.getColumnMetadata().getName()));
            execute(session, connection, sql);
        } catch (SQLException e) {
            throw new TrinoException(JDBC_ERROR, e);
        }
    }

    @Override
    public void truncateTable(ConnectorSession session, JdbcTableHandle handle) {
        throw new TrinoException(NOT_SUPPORTED, "This connector does not support truncating tables");
    }

    @Override
    public void renameTable(ConnectorSession session, JdbcTableHandle handle, SchemaTableName newTableName) {
        SchemaTableName currentName = handle.asPlainTable().getSchemaTableName();
        if (!currentName.getSchemaName().equalsIgnoreCase(newTableName.getSchemaName())) {
            throw new TrinoException(NOT_SUPPORTED, "This connector does not support renaming tables across schemas");
        }
        super.renameTable(session, handle, newTableName);
    }

    @Override
    protected void renameTable(
            ConnectorSession session,
            Connection connection,
            String catalogName,
            String remoteSchemaName,
            String remoteTableName,
            String newRemoteSchemaName,
            String newRemoteTableName
    ) throws SQLException {
        // YDB rename is table-path only; ignore catalog/schema in the SQL.
        execute(session, connection, format(
                "ALTER TABLE %s RENAME TO %s",
                quoted(remoteTableName),
                quoted(newRemoteTableName)));
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
        if (!column.isNullable()) {
            throw new TrinoException(NOT_SUPPORTED, "This connector does not support adding not null columns");
        }
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
        throw new TrinoException(NOT_SUPPORTED, "This connector does not support renaming columns");
    }

    @Override
    @SuppressWarnings("all")
    public List<JdbcColumnHandle> getPrimaryKeys(ConnectorSession session, RemoteTableName remoteTableName) {
        String tableName = remoteTableName.getTableName();

        try (Connection connection = getConnection(session)) {
            DatabaseMetaData metaData = connection.getMetaData();

            String catalogName = remoteTableName.getCatalogName().orElse(null);
            String schemaName = remoteTableName.getSchemaName().orElse(null);

            try (ResultSet primaryKeyResultSet = metaData.getPrimaryKeys(catalogName, schemaName, tableName)) {
                ImmutableList.Builder<JdbcColumnHandle> primaryKeys = ImmutableList.builder();

                while (primaryKeyResultSet.next()) {
                    String columnName = primaryKeyResultSet.getString("COLUMN_NAME");

                    SchemaTableName schemaTableName = new SchemaTableName(
                        schemaName != null ? schemaName : "ydb",
                        tableName
                    );

                    List<JdbcColumnHandle> allColumns = getColumns(session, schemaTableName, remoteTableName);
                    Optional<JdbcColumnHandle> columnHandle = allColumns.stream()
                            .filter(col -> col.getColumnName().equals(columnName))
                            .findFirst();

                    if (columnHandle.isPresent()) {
                        primaryKeys.add(columnHandle.get());
                    }
                }

                List<JdbcColumnHandle> result = primaryKeys.build();
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (SQLException e) {

        }

        // For Trino's temporary tables in tests, where PK's are not created.
        SchemaTableName schemaTableName = new SchemaTableName("ydb", tableName);
        try {
            List<JdbcColumnHandle> allColumns = getColumns(session, schemaTableName, remoteTableName);
            if (!allColumns.isEmpty()) {
                return ImmutableList.of(allColumns.get(0));
            }
        } catch (Exception e) {

        }

        return List.of();
    }

    @Override
    public boolean supportsMerge() {
        return true;
    }

    @Override
    public JdbcMergeTableHandle beginMerge(
            ConnectorSession session,
            JdbcTableHandle handle,
            Map<Integer, Collection<ColumnHandle>> updateColumnHandles,
            List<Runnable> rollbackActions,
            RetryMode retryMode
    ) {
        List<JdbcColumnHandle> primaryKeys = getPrimaryKeys(session, handle.getRequiredNamedRelation().getRemoteTableName());

        SchemaTableName schemaTableName = handle.getRequiredNamedRelation().getSchemaTableName();
        RemoteTableName remoteTableName = handle.getRequiredNamedRelation().getRemoteTableName();

        List<JdbcColumnHandle> columns = getColumns(session, schemaTableName, remoteTableName);

        JdbcTableHandle plainTable = new JdbcTableHandle(schemaTableName, remoteTableName, Optional.empty());

        JdbcOutputTableHandle outputTableHandle = beginInsertTable(session, plainTable, columns);

        return new JdbcMergeTableHandle(
                handle,
                outputTableHandle,
                ImmutableMap.of(),
                Optional.empty(),
                primaryKeys,
                columns,
                updateColumnHandles);
    }

    @Override
    public void finishMerge(
            ConnectorSession session,
            JdbcMergeTableHandle tableHandle,
            Set<Long> pageSinkIds
    ) {
        try (Connection connection = getConnection(session)) {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new TrinoException(JDBC_ERROR, e);
        }
    }

    @Override
    public OptionalLong delete(ConnectorSession session, JdbcTableHandle handle) {
        try (Connection connection = connectionFactory.openConnection(session)) {
            PreparedQuery preparedQuery = queryBuilder.prepareDeleteQuery(
                    this,
                    session,
                    connection,
                    handle.getRequiredNamedRelation(),
                    handle.getConstraint(),
                    getAdditionalPredicate(handle.getConstraintExpressions(), Optional.empty()));

            String deleteSql = preparedQuery.query();
            // Very dirty hack, because it seems like YDB does not return deleted row count.
            // However since this connector is select-oriented, this overhead might not be significant.
            // Anyway, it will be benchmarked later.
            String selectSql = deleteSql.replaceFirst("DELETE FROM", "SELECT COUNT(*) FROM");

            long expectedCount;
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                setParameters(selectStmt, preparedQuery.parameters());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    rs.next();
                    expectedCount = rs.getLong(1);
                }
            }

            try (PreparedStatement preparedStatement = queryBuilder.prepareStatement(this, session, connection, preparedQuery, Optional.empty())) {
                preparedStatement.executeUpdate();
                connection.commit();
            }

            return OptionalLong.of(expectedCount);
        } catch (SQLException e) {
            throw new TrinoException(JDBC_ERROR, e);
        }
    }

    private void setParameters(PreparedStatement stmt, List<QueryParameter> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            QueryParameter param = parameters.get(i);
            Object value = param.getValue().orElse(null);
            if (value == null) {
                stmt.setNull(i + 1, Types.NULL);
            } else if (value instanceof Long l) {
                stmt.setLong(i + 1, l);
            } else if (value instanceof Integer n) {
                stmt.setInt(i + 1, n);
            } else if (value instanceof String s) {
                stmt.setString(i + 1, s);
            } else if (value instanceof Double d) {
                stmt.setDouble(i + 1, d);
            } else if (value instanceof Boolean b) {
                stmt.setBoolean(i + 1, b);
            } else {
                stmt.setObject(i + 1, value);
            }
        }
    }

    @Override
    public OptionalLong update(ConnectorSession session, JdbcTableHandle handle) {
        try (Connection connection = connectionFactory.openConnection(session)) {
            PreparedQuery preparedQuery = queryBuilder.prepareUpdateQuery(
                    this,
                    session,
                    connection,
                    handle.getRequiredNamedRelation(),
                    handle.getConstraint(),
                    getAdditionalPredicate(handle.getConstraintExpressions(), Optional.empty()),
                    handle.getUpdateAssignments());

            String updateSql = preparedQuery.query();
            // Very dirty hack, because it seems like YDB does not return deleted row count.
            // However since this connector is select-oriented, this overhead might not be significant.
            // Anyway, it will be benchmarked later.
            String selectSql = updateSql.replaceFirst("UPDATE\\s+(\\S+)\\s+SET\\s+.*?\\s+WHERE\\s+", "SELECT count(*) FROM $1 WHERE ");

            int setParameterCount = handle.getUpdateAssignments().size();

            long expectedCount;
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                List<QueryParameter> whereParameters = preparedQuery.parameters().subList(setParameterCount, preparedQuery.parameters().size());
                setParameters(selectStmt, whereParameters);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    rs.next();
                    expectedCount = rs.getLong(1);
                }
            }

            try (PreparedStatement preparedStatement = queryBuilder.prepareStatement(this, session, connection, preparedQuery, Optional.empty())) {
                preparedStatement.executeUpdate();
                connection.commit();
            }

            return OptionalLong.of(expectedCount);
        } catch (SQLException e) {
            throw new TrinoException(JDBC_ERROR, e);
        }
    }

    @Override
    protected List<String> createTableSqls(RemoteTableName remoteTableName, List<String> columns, ConnectorTableMetadata tableMetadata) {
        return super.createTableSqls(remoteTableName, columns, tableMetadata);
    }
}