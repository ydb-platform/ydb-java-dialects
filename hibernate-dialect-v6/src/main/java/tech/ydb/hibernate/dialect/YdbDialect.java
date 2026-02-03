package tech.ydb.hibernate.dialect;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.CurrentFunction;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.BasicType;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BIT;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.INTERVAL_SECOND;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.LONGNVARCHAR;
import static org.hibernate.type.SqlTypes.LONGVARBINARY;
import static org.hibernate.type.SqlTypes.LONGVARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import tech.ydb.hibernate.dialect.code.YdbJdbcCode;
import tech.ydb.hibernate.dialect.exporter.EmptyExporter;
import tech.ydb.hibernate.dialect.exporter.YdbIndexExporter;
import tech.ydb.hibernate.dialect.hint.IndexQueryHintHandler;
import tech.ydb.hibernate.dialect.hint.PragmaQueryHintHandler;
import tech.ydb.hibernate.dialect.hint.QueryHintHandler;
import tech.ydb.hibernate.dialect.hint.ScanQueryHintHandler;
import tech.ydb.hibernate.dialect.identity.YdbIdentityColumnSupport;
import tech.ydb.hibernate.dialect.translator.YdbSqlAstTranslatorFactory;
import tech.ydb.hibernate.dialect.types.BigDecimalJavaType;
import tech.ydb.hibernate.dialect.types.DecimalJdbcType;
import tech.ydb.hibernate.dialect.types.InstantJavaType;
import tech.ydb.hibernate.dialect.types.InstantJdbcType;
import tech.ydb.hibernate.dialect.types.LocalDateJavaType;
import tech.ydb.hibernate.dialect.types.LocalDateJdbcType;
import tech.ydb.hibernate.dialect.types.LocalDateTimeJavaType;
import tech.ydb.hibernate.dialect.types.LocalDateTimeJdbcType;
import tech.ydb.hibernate.dialect.types.YdbJdbcType;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDialect extends Dialect {
    private static final Exporter<ForeignKey> FOREIGN_KEY_EMPTY_EXPORTER = new EmptyExporter<>();
    private static final Exporter<Constraint> UNIQUE_KEY_EMPTY_EXPORTER = new EmptyExporter<>();
    private static final List<QueryHintHandler> QUERY_HINT_HANDLERS = List.of(
            IndexQueryHintHandler.INSTANCE,
            ScanQueryHintHandler.INSTANCE,
            PragmaQueryHintHandler.INSTANCE
    );
    private static final ConcurrentHashMap<Integer, DecimalJdbcType> DECIMAL_JDBC_TYPE_CACHE = new ConcurrentHashMap<>();

    public YdbDialect(DialectResolutionInfo dialectResolutionInfo) {
        super(dialectResolutionInfo);
    }

    @Override
    protected String columnType(int sqlTypeCode) {
        return switch (sqlTypeCode) {
            case BOOLEAN, BIT -> "Bool";
            case TINYINT -> "Int8";
            case SMALLINT -> "Int16";
            case INTEGER -> "Int32";
            case BIGINT -> "Int64";
            case REAL, FLOAT -> "Float";
            case DOUBLE -> "Double";
            case NUMERIC, DECIMAL -> "Decimal($p, $s)";
            case DATE -> "Date";
            case INTERVAL_SECOND -> "Interval";
            case TIME_WITH_TIMEZONE -> "TzDateTime";
            case TIMESTAMP, TIMESTAMP_UTC -> "Timestamp";
            case TIMESTAMP_WITH_TIMEZONE -> "TzTimestamp";
            case CHAR, VARCHAR, CLOB, NCHAR, NVARCHAR, NCLOB,
                    LONG32VARCHAR, LONG32NVARCHAR, LONGVARCHAR, LONGNVARCHAR -> "Text";
            case BINARY, VARBINARY, BLOB, LONGVARBINARY, LONG32VARBINARY -> "Bytes";
            case JSON -> "Json";
            case UUID, YdbJdbcCode.UUID -> "Uuid";
            default -> super.columnType(sqlTypeCode);
        };
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);

        typeContributions.contributeJavaType(UUIDJavaType.INSTANCE);
        typeContributions.contributeJdbcType(UUIDJdbcType.INSTANCE);
        typeContributions.contributeJavaType(LocalDateTimeJavaType.INSTANCE);
        typeContributions.contributeJdbcType(LocalDateTimeJdbcType.INSTANCE);
        typeContributions.contributeJavaType(LocalDateJavaType.INSTANCE);
        typeContributions.contributeJdbcType(LocalDateJdbcType.INSTANCE);
        typeContributions.contributeJavaType(InstantJavaType.INSTANCE);
        typeContributions.contributeJdbcType(InstantJdbcType.INSTANCE);

        // custom jdbc codec
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.UINT8, Integer.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.DATE_32, LocalDate.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.DATETIME_64, LocalDateTime.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.TIMESTAMP_64, Instant.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.INTERVAL_64, Duration.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.JSON, String.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.JSON_DOCUMENT, String.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.BOOL, Boolean.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.INT8, Integer.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.INT16, Integer.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.UINT16, Integer.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.INT32, Integer.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.UINT32, Integer.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.INT64, Integer.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.UINT64, Integer.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.FLOAT, Float.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.DOUBLE, Double.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.BYTES, byte[].class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.TEXT, String.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.YSON, byte[].class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.JSON, String.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.UUID, java.util.UUID.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.DATE, LocalDate.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.DATETIME, LocalDateTime.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.TIMESTAMP, Instant.class));
        typeContributions.contributeJdbcType(new YdbJdbcType(YdbJdbcCode.INTERVAL, Duration.class));

        typeContributions.contributeJavaType(BigDecimalJavaType.INSTANCE_22_9);
        typeContributions.contributeJdbcType(new DecimalJdbcType(YdbJdbcCode.DECIMAL_22_9));
        typeContributions.contributeJdbcType(new DecimalJdbcType(YdbJdbcCode.DECIMAL_31_9));
        typeContributions.contributeJdbcType(new DecimalJdbcType(YdbJdbcCode.DECIMAL_35_0));
        typeContributions.contributeJdbcType(new DecimalJdbcType(YdbJdbcCode.DECIMAL_35_9));
    }

    @Override
    protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.registerColumnTypes(typeContributions, serviceRegistry);

        final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(UUID, "Uuid", "Uuid", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(INTERVAL_SECOND, "Interval", "Interval", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.INTERVAL, "Interval", "Interval", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.DATETIME, "Datetime", "Datetime", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.UINT8, "Uint8", "Uint8", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.DATE_32, "Date32", "Date32", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.DATETIME_64, "Datetime64", "Datetime64", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.TIMESTAMP_64, "Timestamp64", "Timestamp64", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.INTERVAL_64, "Interval64", "Interval64", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.DECIMAL_22_9, "Decimal(22, 9)", "Decimal(22, 9)", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.DECIMAL_31_9, "Decimal(31, 9)", "Decimal(31, 9)", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.DECIMAL_35_0, "Decimal(35, 0)", "Decimal(35, 0)", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.DECIMAL_35_9, "Decimal(35, 9)", "Decimal(35, 9)", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.JSON, "Json", "Json", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.JSON_DOCUMENT, "JsonDocument", "JsonDocument", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.BOOL, "Bool", "Bool", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.INT8, "Int8", "Int8", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.INT16, "Int16", "Int16", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.UINT16, "Uint16", "Uint16", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.INT32, "Int32", "Int32", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.UINT32, "Uint32", "Uint32", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.INT64, "Int64", "Int64", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.UINT64, "Uint64", "Uint64", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.FLOAT, "Float", "Float", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.DOUBLE, "Double", "Double", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.BYTES, "Bytes", "Bytes", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.TEXT, "Text", "Text", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.YSON, "Yson", "Yson", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.UUID, "Uuid", "Uuid", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.DATE, "Date", "Date", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.DATETIME, "Datetime", "Datetime", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.TIMESTAMP, "Timestamp", "Timestamp", this));
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(YdbJdbcCode.INTERVAL, "Interval", "Interval", this));
    }

    @Override
    public JdbcType resolveSqlTypeDescriptor(
            String columnTypeName,
            int jdbcTypeCode,
            int precision,
            int scale,
            JdbcTypeRegistry jdbcTypeRegistry) {
        if ((jdbcTypeCode == NUMERIC || jdbcTypeCode == DECIMAL) && (precision != 0 || scale != 0)) {
            int sqlCode = ydbDecimal(precision, scale);

            return DECIMAL_JDBC_TYPE_CACHE.computeIfAbsent(sqlCode, DecimalJdbcType::new);
        }

        return super.resolveSqlTypeDescriptor(columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry);
    }

    @Override
    public int getDefaultDecimalPrecision() {
        return 22;
    }

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);

        final BasicType<LocalDateTime> localDateTimeType = functionContributions
                .getTypeConfiguration()
                .getBasicTypeRegistry()
                .resolve(StandardBasicTypes.LOCAL_DATE_TIME);

        functionContributions.getFunctionRegistry().register(
                "current_time",
                new CurrentFunction("current_time", currentTime(), localDateTimeType)
        );
    }

    @Override
    public String addSqlHintOrComment(String sql, QueryOptions queryOptions, boolean commentsEnabled) {
        if (queryOptions.getDatabaseHints() != null) {
            for (var queryHintHandler : QUERY_HINT_HANDLERS) {
                sql = queryHintHandler.addQueryHints(sql, queryOptions.getDatabaseHints());
            }
        }

        if (queryOptions.getComment() != null) {
            boolean commentIsHint = false;

            var hints = queryOptions.getComment().split(";");

            for (var queryHintHandler : QUERY_HINT_HANDLERS) {
                for (var hint : hints) {
                    hint = hint.trim();
                    if (queryHintHandler.commentIsHint(hint)) {
                        commentIsHint = true;
                        sql = queryHintHandler.addQueryHints(sql, List.of(hint));
                    }
                }
            }

            if (commentIsHint) {
                return sql;
            }
        }

        if (commentsEnabled && queryOptions.getComment() != null) {
            sql = prependComment(sql, queryOptions.getComment());
        }

        return sql;
    }

    @Override
    public LimitHandler getLimitHandler() {
        return LimitOffsetLimitHandler.INSTANCE;
    }

    @Override
    public boolean supportsCaseInsensitiveLike() {
        return true;
    }

    @Override
    public String getCaseInsensitiveLike() {
        return "ilike";
    }

    @Override
    public char openQuote() {
        return '`';
    }

    @Override
    public char closeQuote() {
        return '`';
    }

    @Override
    public boolean supportsLockTimeouts() {
        return false;
    }

    @Override
    public boolean supportsTemporaryTables() {
        return false;
    }

    @Override
    public boolean supportsOffsetInSubquery() {
        return true;
    }

    @Override
    public boolean supportsValuesList() {
        return true;
    }

    @Override
    public boolean supportsDistinctFromPredicate() {
        return true;
    }

    @Override
    public boolean supportsPartitionBy() {
        return true;
    }

    @Override
    public boolean supportsExistsInSelect() {
        return false;
    }

    @Override
    public void appendBooleanValueString(SqlAppender appender, boolean bool) {
        appender.append(toBooleanValueString(bool));
    }

    @Override
    public String toBooleanValueString(boolean bool) {
        return String.valueOf(bool);
    }

    @Override
    public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        return YdbSqlAstTranslatorFactory.YDB_SQL_AST_TRANSLATOR_FACTORY;
    }

    @Override
    public Exporter<Index> getIndexExporter() {
        return YdbIndexExporter.INSTANCE;
    }

    @Override
    public String currentDate() {
        return "CurrentUtcDate()";
    }

    @Override
    public String currentTime() {
        return "CurrentUtcDatetime()";
    }

    @Override
    public String currentTimestamp() {
        return "CurrentUtcTimestamp()";
    }

    @Override
    public String currentTimestampWithTimeZone() {
        return "CurrentTzTimestamp()";
    }

    @Override
    public boolean isCurrentTimestampSelectStringCallable() {
        return false;
    }

    @Override
    public String getCurrentTimestampSelectString() {
        return "select " + currentTimestamp();
    }

    @Override
    public String getForUpdateString() {
        throw new UnsupportedOperationException("YDB does not support FOR UPDATE statement");
    }

    @Override
    public boolean supportsOuterJoinForUpdate() {
        return false;
    }

    @Override
    public boolean dropConstraints() {
        return false;
    }

    @Override
    public Exporter<Constraint> getUniqueKeyExporter() {
        return UNIQUE_KEY_EMPTY_EXPORTER;
    }

    @Override
    public Exporter<ForeignKey> getForeignKeyExporter() {
        return FOREIGN_KEY_EMPTY_EXPORTER;
    }

    @Override
    public boolean supportsColumnCheck() {
        return false;
    }

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return YdbIdentityColumnSupport.INSTANCE;
    }

    @Override
    public boolean supportsInsertReturning() {
        return true;
    }

    @Override
    public boolean supportsInsertReturningGeneratedKeys() {
        return true;
    }

    private static int ydbDecimal(int precision, int scale) {
        return 1 << 14 + (precision << 6) + (scale & 0x111111);
    }
}
