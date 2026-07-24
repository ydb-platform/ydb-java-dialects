package tech.ydb.hibernate.dialect;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import static org.hibernate.internal.util.JdbcExceptionHelper.extractErrorCode;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.tool.schema.spi.Exporter;
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
import org.hibernate.type.spi.TypeConfiguration;
import tech.ydb.hibernate.dialect.code.YdbJdbcCode;
import tech.ydb.hibernate.dialect.exporter.EmptyExporter;
import tech.ydb.hibernate.dialect.exporter.YdbIndexExporter;
import tech.ydb.hibernate.dialect.hint.QueryHints;
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
    @SuppressWarnings({"rawtypes"})
    private static final Exporter EMPTY_EXPORTER = new EmptyExporter<>();
    private static final ConcurrentHashMap<Integer, DecimalJdbcType> DECIMAL_JDBC_TYPE_CACHE = new ConcurrentHashMap<>();

    public YdbDialect(DialectResolutionInfo dialectResolutionInfo) {
        super(dialectResolutionInfo);
    }

    @Override
    protected String columnType(int sqlTypeCode) {
        switch (sqlTypeCode) {
            case BOOLEAN:
            case BIT:
                return "Bool";
            case TINYINT:
                return "Int8";
            case SMALLINT:
                return "Int16";
            case INTEGER:
                return "Int32";
            case BIGINT:
                return "Int64";
            case REAL:
            case FLOAT:
                return "Float";
            case DOUBLE:
                return "Double";
            case NUMERIC:
            case DECIMAL:
                return "Decimal($p, $s)";
            case DATE:
                return "Date";
            case INTERVAL_SECOND:
                return "Interval";
            case TIME_WITH_TIMEZONE:
                return "TzDateTime";
            case TIMESTAMP:
            case TIMESTAMP_UTC:
                return "Timestamp";
            case TIMESTAMP_WITH_TIMEZONE:
                return "TzTimestamp";
            case CHAR:
            case VARCHAR:
            case CLOB:
            case NCHAR:
            case NVARCHAR:
            case NCLOB:
            case LONG32VARCHAR:
            case LONG32NVARCHAR:
            case LONGVARCHAR:
            case LONGNVARCHAR:
                return "Text";
            case BINARY:
            case VARBINARY:
            case BLOB:
            case LONGVARBINARY:
            case LONG32VARBINARY:
                return "Bytes";
            case JSON:
                return "Json";
            case UUID:
            case YdbJdbcCode.UUID:
                return "Uuid";
            default:
                return super.columnType(sqlTypeCode);
        }
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

        final SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        final TypeConfiguration typeConfig = functionContributions.getTypeConfiguration();

        functionRegistry.registerPattern(
                "lower",
                "Unicode::ToLower(?1)"
        );

        functionRegistry.registerPattern(
                "upper",
                "Unicode::ToUpper(?1)"
        );

        functionRegistry.patternDescriptorBuilder("concat", "(?1||?2...)")
                .setInvariantType(typeConfig.getBasicTypeRegistry().resolve(StandardBasicTypes.STRING))
                .setMinArgumentCount(1)
                .setArgumentTypeResolver(
                        StandardFunctionArgumentTypeResolvers.impliedOrInvariant(typeConfig, FunctionParameterType.STRING))
                .setArgumentListSignature("(STRING string0[, STRING string1[, ...]])")
                .register();
    }

    @Override
    public String addSqlHintOrComment(String sql, QueryOptions queryOptions, boolean commentsEnabled) {
        if (queryOptions.getDatabaseHints() != null) {
            String sqlWithDatabaseHints = applyQueryHints(sql, queryOptions.getDatabaseHints());
            if (sqlWithDatabaseHints != null) {
                sql = sqlWithDatabaseHints;
            }
        }

        String comment = queryOptions.getComment();
        if (comment == null) {
            return sql;
        }

        String sqlWithCommentHits = applyQueryHints(sql, Arrays.asList(comment.split(";")));
        if (sqlWithCommentHits != null) {
            return sqlWithCommentHits;
        }

        return commentsEnabled ? prependComment(sql, comment) : sql;
    }

    private static String applyQueryHints(String sql, List<String> hints) {
        List<String> indexes = new ArrayList<>();
        List<String> pragmas = new ArrayList<>();
        boolean scan = false;

        for (String hint : hints) {
            hint = hint.trim();
            scan = scan || hint.equals("use_scan");

            addHintValueIfMatches(hint, "use_index:", indexes);
            addHintValueIfMatches(hint, "add_pragma:", pragmas);
        }

        if (indexes.isEmpty() && !scan && pragmas.isEmpty()) {
            return null;
        }

        if (!indexes.isEmpty()) {
            sql = QueryHints.addViewIndexesToQuery(sql, indexes);
        }
        if (scan) {
            sql = QueryHints.addScanToQuery(sql);
        }
        if (!pragmas.isEmpty()) {
            sql = QueryHints.addPragmasToQuery(sql, pragmas);
        }

        return sql;
    }

    private static void addHintValueIfMatches(String hint, String prefix, List<String> target) {
        if (hint.startsWith(prefix)) {
            target.add(hint.substring(prefix.length()));
        }
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
        return "";
    }

    @Override
    public boolean supportsOuterJoinForUpdate() {
        return false;
    }

    @Override
    public boolean dropConstraints() {
        return false;
    }

    @SuppressWarnings({"rawtypes", "override"})
    @Override
    public Exporter getUniqueKeyExporter() {
        return EMPTY_EXPORTER;
    }

    @Override
    public Exporter<ForeignKey> getForeignKeyExporter() {
        return EMPTY_EXPORTER;
    }

    @Override
    public boolean supportsOrdinalSelectItemReference() {
        return false;
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

    @Override
    public void appendLiteral(SqlAppender appender, String literal) {
        super.appendLiteral(appender, literal);
        appender.append('u');
    }

    @Override
    public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
        return (sqlException, message, sql) -> {
            String msg = sqlException.getMessage();
            int errorCode = extractErrorCode(sqlException);
            if (errorCode == 400120 && msg != null && msg.contains("Conflict with existing key")) {
                return new ConstraintViolationException(message, sqlException, sql, null);
            }
            return null;
        };
    }

    private static int ydbDecimal(int precision, int scale) {
        return 1 << 14 + (precision << 6) + (scale & 0x111111);
    }
}
