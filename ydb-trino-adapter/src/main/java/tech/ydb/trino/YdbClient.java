package tech.ydb.trino;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import io.trino.plugin.base.aggregation.AggregateFunctionRewriter;
import io.trino.plugin.base.aggregation.AggregateFunctionRule;
import io.trino.plugin.base.expression.ConnectorExpressionRewriter;
import io.trino.plugin.base.mapping.IdentifierMapping;
import io.trino.plugin.base.mapping.RemoteIdentifiers;
import io.trino.plugin.base.projection.ProjectFunctionRewriter;
import io.trino.plugin.base.projection.ProjectFunctionRule;
import io.trino.plugin.jdbc.BaseJdbcClient;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.BooleanWriteFunction;
import io.trino.plugin.jdbc.ColumnMapping;
import io.trino.plugin.jdbc.ConnectionFactory;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcExpression;
import io.trino.plugin.jdbc.JdbcMergeTableHandle;
import io.trino.plugin.jdbc.JdbcOutputTableHandle;
import io.trino.plugin.jdbc.JdbcSortItem;
import io.trino.plugin.jdbc.JdbcTableHandle;
import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.PreparedQuery;
import io.trino.plugin.jdbc.QueryBuilder;
import io.trino.plugin.jdbc.RemoteTableName;
import io.trino.plugin.jdbc.WriteMapping;
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
import io.trino.spi.TrinoException;
import io.trino.spi.connector.AggregateFunction;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableMetadata;
import io.trino.spi.connector.RelationCommentMetadata;
import io.trino.spi.connector.RetryMode;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.connector.SortOrder;
import io.trino.spi.connector.TableNotFoundException;
import io.trino.spi.expression.ConnectorExpression;
import io.trino.spi.type.DecimalType;
import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;

import jakarta.annotation.Nullable;
import tech.ydb.jdbc.YdbConnection;
import tech.ydb.jdbc.context.YdbContext;
import tech.ydb.scheme.description.Entry;
import tech.ydb.scheme.description.EntryType;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.trino.plugin.jdbc.DefaultJdbcMetadata.MERGE_ROW_ID;
import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;
import static io.trino.plugin.jdbc.PredicatePushdownController.DISABLE_PUSHDOWN;
import static io.trino.plugin.jdbc.PredicatePushdownController.FULL_PUSHDOWN;
import static io.trino.plugin.jdbc.StandardColumnMappings.bigintColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.bigintWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.booleanColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.dateWriteFunctionUsingLocalDate;
import static io.trino.plugin.jdbc.StandardColumnMappings.dateReadFunctionUsingLocalDate;
import static io.trino.plugin.jdbc.StandardColumnMappings.decimalColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.doubleColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.doubleWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.integerColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.integerWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.longDecimalWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.realColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.realWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.shortDecimalWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.smallintColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.smallintWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.timestampColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.timestampReadFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.timestampWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.tinyintWriteFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.varcharColumnMapping;
import static io.trino.plugin.jdbc.StandardColumnMappings.varcharReadFunction;
import static io.trino.plugin.jdbc.StandardColumnMappings.varcharWriteFunction;
import static io.trino.spi.StandardErrorCode.AMBIGUOUS_NAME;
import static io.trino.spi.StandardErrorCode.ALREADY_EXISTS;
import static io.trino.spi.StandardErrorCode.INVALID_ARGUMENTS;
import static io.trino.spi.StandardErrorCode.NOT_SUPPORTED;
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
    static final String DEFAULT_SCHEMA = "default";
    private static final int YDB_DEFAULT_DECIMAL_PRECISION = 22;
    private static final int YDB_DEFAULT_DECIMAL_SCALE = 9;

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
    public Optional<JdbcExpression> convertProjection(
            ConnectorSession session,
            JdbcTableHandle handle,
            ConnectorExpression expression,
            Map<String, ColumnHandle> assignments
    ) {
        if (!handle.getUpdateAssignments().isEmpty() || assignments.values().stream()
                .filter(JdbcColumnHandle.class::isInstance)
                .map(JdbcColumnHandle.class::cast)
                .anyMatch(column -> column.getColumnName().equals(MERGE_ROW_ID))) {
            return Optional.empty();
        }

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
        return ImmutableSet.of(DEFAULT_SCHEMA);
    }

    @Override
    protected String escapeObjectNameForMetadataQuery(String name, String escape) {
        return name;
    }

    @Override
    public List<SchemaTableName> getTableNames(ConnectorSession session, Optional<String> schema) {
        if (schema.isPresent() && !DEFAULT_SCHEMA.equalsIgnoreCase(schema.get())) {
            return ImmutableList.of();
        }

        try (Connection connection = connectionFactory.openConnection(session)) {
            List<String> paths = listRemoteTablePaths(connection);
            throwIfAmbiguous(paths);
            return paths.stream()
                    .distinct()
                    .map(path -> new SchemaTableName(DEFAULT_SCHEMA, path))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new TrinoException(JDBC_ERROR, "Failed to list YDB tables", e);
        }
    }

    @Override
    public List<RelationCommentMetadata> getAllTableComments(ConnectorSession session, Optional<String> schema) {
        return getTableNames(session, schema).stream()
                .map(table -> RelationCommentMetadata.forRelation(table, Optional.empty()))
                .toList();
    }

    @Override
    public Optional<JdbcTableHandle> getTableHandle(
            ConnectorSession session,
            SchemaTableName schemaTableName
    ) {
        if (!DEFAULT_SCHEMA.equalsIgnoreCase(schemaTableName.getSchemaName())) {
            return Optional.empty();
        }
        String requestedPath = schemaTableName.getTableName();
        if (!isRelativeTablePath(requestedPath)) {
            return Optional.empty();
        }

        try (Connection connection = connectionFactory.openConnection(session)) {
            return resolveRemoteTablePath(connection, requestedPath)
                    .map(remotePath -> new JdbcTableHandle(
                            new SchemaTableName(DEFAULT_SCHEMA, schemaTableName.getTableName()),
                            new RemoteTableName(Optional.empty(), Optional.empty(), remotePath),
                            Optional.empty()));
        } catch (SQLException e) {
            throw new TrinoException(JDBC_ERROR, "Failed to resolve YDB table " + schemaTableName, e);
        }
    }

    private List<String> listRemoteTablePaths(Connection connection)
            throws SQLException
    {
        ImmutableList.Builder<String> paths = ImmutableList.builder();
        try (ResultSet resultSet = getTables(connection, Optional.empty(), Optional.empty())) {
            while (resultSet.next()) {
                paths.add(resultSet.getString("TABLE_NAME"));
            }
        }
        return paths.build();
    }

    private Optional<String> resolveRemoteTablePath(Connection connection, String requestedPath)
            throws SQLException
    {
        List<String> matches = listRemoteTablePaths(connection).stream()
                .filter(path -> path.toLowerCase(Locale.ROOT).equals(requestedPath.toLowerCase(Locale.ROOT)))
                .distinct()
                .toList();
        throwIfAmbiguous(matches);
        return matches.stream().findFirst();
    }

    private static void throwIfAmbiguous(List<String> paths) {
        Map<String, String> names = new TreeMap<>();
        for (String path : paths) {
            String previous = names.putIfAbsent(path.toLowerCase(Locale.ROOT), path);
            if (previous != null && !previous.equals(path)) {
                throw new TrinoException(AMBIGUOUS_NAME, "Ambiguous YDB table paths: " + previous + ", " + path);
            }
        }
    }

    private static boolean isRelativeTablePath(String path) {
        for (String part : path.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                return false;
            }
        }
        return true;
    }

    private static String validateTablePath(String path) {
        if (!isRelativeTablePath(path)) {
            throw new TrinoException(INVALID_ARGUMENTS, "Invalid YDB table path '" + path + "': expected a relative path without empty, '.' or '..' components");
        }
        return path;
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
            case Types.FLOAT -> Optional.of(jdbcTypeName.equals("float") ? realColumnMapping() : doubleColumnMapping());
            case Types.DOUBLE -> Optional.of(doubleColumnMapping());
            case Types.DECIMAL -> {
                String typeName = typeHandle.jdbcTypeName().orElse("Decimal");
                int precision = typeHandle.columnSize().orElse(YDB_DEFAULT_DECIMAL_PRECISION);
                int scale = typeHandle.decimalDigits().orElse(YDB_DEFAULT_DECIMAL_SCALE);
                int start = typeName.indexOf('(');
                int end = typeName.indexOf(')');
                if (start >= 0 && end > start) {
                    String[] parts = typeName.substring(start + 1, end).split(",");
                    if (parts.length == 2) {
                        Integer typeNamePrecision = Ints.tryParse(parts[0].trim());
                        Integer typeNameScale = Ints.tryParse(parts[1].trim());
                        if (typeNamePrecision != null && typeNameScale != null) {
                            precision = typeNamePrecision;
                            scale = typeNameScale;
                        }
                    }
                }

                DecimalType decimalType = createDecimalType(precision, max(scale, 0));
                ColumnMapping decimalMapping = decimalColumnMapping(decimalType);
                yield Optional.of(ColumnMapping.mapping(
                        decimalType,
                        decimalMapping.getReadFunction(),
                        decimalMapping.getWriteFunction(),
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

    @Override
    public String quoted(String name) {
        return "`" + name.replace("\\", "\\\\").replace("`", "\\`") + "`";
    }

    @Override
    protected String quoted(@Nullable String catalog, @Nullable String schema, String table) {
        // YDB doesn't use catalog & schema in table names, only the table path
        return quoted(validateTablePath(table));
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
    public void setColumnType(ConnectorSession session, JdbcTableHandle handle, JdbcColumnHandle column, Type type) {
        throw new TrinoException(NOT_SUPPORTED, "This connector does not support setting column types");
    }

    @Override
    public void truncateTable(ConnectorSession session, JdbcTableHandle handle) {
        throw new TrinoException(NOT_SUPPORTED, "This connector does not support truncating tables");
    }

    @Override
    protected JdbcOutputTableHandle createTable(
            ConnectorSession session,
            Connection connection,
            ConnectorTableMetadata tableMetadata,
            RemoteIdentifiers remoteIdentifiers,
            String catalog,
            String remoteSchema,
            String remoteTable,
            String remoteTargetTableName,
            Optional<ColumnMetadata> pageSinkIdColumn)
            throws SQLException {
        // Validate the logical CTAS destination before creating its temporary table.
        String destination = resolveDestinationPath(connection, remoteTable);
        String target = remoteTargetTableName.equals(remoteTable)
                ? destination
                : remoteTargetTableName;
        return super.createTable(session, connection, tableMetadata, remoteIdentifiers,
                catalog, remoteSchema, destination, target, pageSinkIdColumn);
    }

    private String resolveDestinationPath(Connection connection, String name) throws SQLException {
        String[] components = validateTablePath(name).split("/");
        YdbContext context = connection.unwrap(YdbConnection.class).getCtx();
        String parent = context.getGrpcTransport().getDatabase();
        StringBuilder relative = new StringBuilder();
        for (int index = 0; index < components.length; index++) {
            List<Entry> children;
            try {
                // The JDBC context owns this client; do not close it independently.
                children = context.getSchemeClient().listDirectory(parent).join().getValue().getEntryChildren();
            }
            catch (RuntimeException e) {
                throw new TrinoException(JDBC_ERROR, "Failed to resolve parent of YDB table path '" + name + "'", e);
            }
            String component = components[index];
            List<Entry> matches = children.stream()
                    .filter(entry -> entry.getName().equalsIgnoreCase(component))
                    .toList();
            if (matches.size() > 1) {
                throw new TrinoException(AMBIGUOUS_NAME, "Ambiguous YDB path component '" + component + "' in '" + name + "'");
            }
            if (index == components.length - 1) {
                if (!matches.isEmpty()) {
                    throw new TrinoException(ALREADY_EXISTS, "YDB object already exists at table path '" + name + "'");
                }
                return relative.append(component).toString();
            }
            if (matches.isEmpty() || matches.getFirst().getType() != EntryType.DIRECTORY) {
                throw new TrinoException(INVALID_ARGUMENTS, "YDB parent directory does not exist for table path '" + name + "'");
            }
            String spelling = matches.getFirst().getName();
            relative.append(spelling).append('/');
            parent = parent.endsWith("/") ? parent + spelling : parent + "/" + spelling;
        }
        throw new IllegalStateException("Empty validated YDB path");
    }

    @Override
    protected void renameTable(ConnectorSession session, Connection connection, String catalogName,
            String remoteSchemaName, String remoteTableName, String newRemoteSchemaName, String newRemoteTableName)
            throws SQLException {
        super.renameTable(session, connection, catalogName, remoteSchemaName, remoteTableName,
                newRemoteSchemaName, resolveDestinationPath(connection, newRemoteTableName));
    }

    @Override
    protected JdbcOutputTableHandle beginInsertTable(
            ConnectorSession session, Connection connection, RemoteIdentifiers remoteIdentifiers,
            String catalog, String remoteSchema, String remoteTable, List<JdbcColumnHandle> columns)
            throws SQLException {
        String table = resolveRemoteTablePath(connection, remoteTable)
                .orElseThrow(() -> new TableNotFoundException(new SchemaTableName(DEFAULT_SCHEMA, remoteTable)));
        return super.beginInsertTable(session, connection, remoteIdentifiers, catalog, remoteSchema, table, columns);
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
    protected String getColumnDefinitionSql(ConnectorSession session, ColumnMetadata column, String columnName) {
        // YDB restriction, does not support column comments.
        if (column.getComment().filter(comment -> !comment.isEmpty()).isPresent()) {
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
        super.addColumn(session, connection, table, column);
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
    public List<JdbcColumnHandle> getPrimaryKeys(ConnectorSession session, RemoteTableName remoteTableName) {
        String tableName = remoteTableName.getTableName();
        String metadataSchemaName = remoteTableName.getSchemaName().orElse(null);
        SchemaTableName schemaTableName = new SchemaTableName(
                remoteTableName.getSchemaName().orElse(DEFAULT_SCHEMA),
                tableName);
        List<JdbcColumnHandle> columns = getColumnsForPrimaryKeyLookup(session, schemaTableName, remoteTableName);
        Map<String, JdbcColumnHandle> columnsByName = columns.stream()
                .collect(Collectors.toMap(JdbcColumnHandle::getColumnName, Function.identity()));

        try (Connection connection = getConnection(session)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalogName = remoteTableName.getCatalogName().orElse(null);

            try (ResultSet primaryKeyResultSet = metaData.getPrimaryKeys(catalogName, metadataSchemaName, tableName)) {
                Map<Short, JdbcColumnHandle> primaryKeysBySequence = new TreeMap<>();

                while (primaryKeyResultSet.next()) {
                    String columnName = primaryKeyResultSet.getString("COLUMN_NAME");
                    JdbcColumnHandle column = columnsByName.get(columnName);
                    if (column == null) {
                        throw new TrinoException(
                                JDBC_ERROR,
                                "Primary key column '%s' is absent from JDBC column metadata for %s"
                                        .formatted(columnName, remoteTableName));
                    }
                    short keySequence = primaryKeyResultSet.getShort("KEY_SEQ");
                    if (primaryKeysBySequence.put(keySequence, column) != null) {
                        throw new TrinoException(
                                JDBC_ERROR,
                                "Duplicate primary key sequence %s for %s".formatted(keySequence, remoteTableName));
                    }
                }
                return ImmutableList.copyOf(primaryKeysBySequence.values());
            }
        }
        catch (SQLException e) {
            throw new TrinoException(JDBC_ERROR, "Failed to read primary key metadata for " + remoteTableName, e);
        }
    }

    protected List<JdbcColumnHandle> getColumnsForPrimaryKeyLookup(
            ConnectorSession session,
            SchemaTableName schemaTableName,
            RemoteTableName remoteTableName) {
        return getColumns(session, schemaTableName, remoteTableName);
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
            Consumer<Runnable> rollbackActionCollector,
            RetryMode retryMode
    ) {
        if (retryMode != RetryMode.NO_RETRIES) {
            throw new TrinoException(NOT_SUPPORTED, "Query and task retries are not supported for direct YDB MERGE");
        }

        List<JdbcColumnHandle> primaryKeys = getPrimaryKeys(session, handle.getRequiredNamedRelation().getRemoteTableName());
        if (primaryKeys.isEmpty()) {
            throw new TrinoException(NOT_SUPPORTED, "The connector cannot perform MERGE on a table without a primary key");
        }

        SchemaTableName schemaTableName = handle.getRequiredNamedRelation().getSchemaTableName();
        RemoteTableName remoteTableName = handle.getRequiredNamedRelation().getRemoteTableName();
        RemoteTableName outputRemoteTableName = new RemoteTableName(
                remoteTableName.getCatalogName(),
                Optional.of(schemaTableName.getSchemaName()),
                remoteTableName.getTableName());

        List<JdbcColumnHandle> columns = getColumns(session, schemaTableName, remoteTableName);

        JdbcOutputTableHandle outputTableHandle = new JdbcOutputTableHandle(
                outputRemoteTableName,
                columns.stream().map(JdbcColumnHandle::getColumnName).toList(),
                columns.stream().map(JdbcColumnHandle::getColumnType).toList(),
                Optional.of(columns.stream().map(JdbcColumnHandle::getJdbcTypeHandle).toList()),
                Optional.empty(),
                Optional.empty());

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
        // Each YdbMergeSink owns and commits its transaction before reporting success.
    }

    @Override
    public OptionalInt getMaxWriteParallelism(ConnectorSession session) {
        // Direct-to-target merge sinks cannot be committed atomically across writer tasks.
        return OptionalInt.of(1);
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
            return OptionalLong.of(executeReturningDml(session, connection, handle, preparedQuery));
        }
        catch (SQLException e) {
            throw new TrinoException(JDBC_ERROR, e);
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
            return OptionalLong.of(executeReturningDml(session, connection, handle, preparedQuery));
        }
        catch (SQLException e) {
            throw new TrinoException(JDBC_ERROR, e);
        }
    }

    private long executeReturningDml(
            ConnectorSession session,
            Connection connection,
            JdbcTableHandle handle,
            PreparedQuery preparedQuery) throws SQLException {
        List<JdbcColumnHandle> primaryKeys = getPrimaryKeys(
                session,
                handle.getRequiredNamedRelation().getRemoteTableName());
        if (primaryKeys.isEmpty()) {
            throw new TrinoException(NOT_SUPPORTED, "YDB DML requires a table primary key");
        }
        PreparedQuery returningQuery = preparedQuery.transformQuery(
                query -> query + " RETURNING " + quoted(primaryKeys.getFirst().getColumnName()));

        long affectedRows = 0;
        try (PreparedStatement statement = queryBuilder.prepareStatement(this, session, connection, returningQuery, Optional.of(1));
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                affectedRows++;
            }
        }
        return affectedRows;
    }

    @Override
    protected List<String> createTableSqls(RemoteTableName remoteTableName, List<String> columns, ConnectorTableMetadata tableMetadata) {
        return super.createTableSqls(remoteTableName, columns, tableMetadata);
    }
}
