package tech.ydb.hibernate.dialect;

import java.time.LocalDateTime;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.CurrentFunction;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
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
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import org.hibernate.type.StandardBasicTypes;
import tech.ydb.hibernate.dialect.exporter.EmptyExporter;
import tech.ydb.hibernate.dialect.exporter.YdbIndexExporter;
import tech.ydb.hibernate.dialect.translator.YdbSqlAstTranslatorFactory;
import tech.ydb.hibernate.dialect.types.LocalDateTimeJavaType;
import tech.ydb.hibernate.dialect.types.LocalDateTimeJdbcType;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDialect extends Dialect {

    private static final Exporter<ForeignKey> FOREIGN_KEY_EMPTY_EXPORTER = new EmptyExporter<>();
    private static final Exporter<Constraint> UNIQUE_KEY_EMPTY_EXPORTER = new EmptyExporter<>();

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
            case NUMERIC, DECIMAL -> "Decimal (22,9)"; // Fixed
            case DATE -> "Date";
            case TIME -> "Datetime";
            case TIME_WITH_TIMEZONE -> "TzDateTime";
            case TIMESTAMP -> "Timestamp";
            case TIMESTAMP_WITH_TIMEZONE -> "TzTimestamp";
            case CHAR, VARCHAR, CLOB, NCHAR, NVARCHAR, NCLOB,
                    LONG32VARCHAR, LONG32NVARCHAR, LONGVARCHAR, LONGNVARCHAR -> "Text";
            case BINARY, VARBINARY, BLOB, LONGVARBINARY, LONG32VARBINARY -> "Bytes";
            case JSON -> "Json";
            default -> super.columnType(sqlTypeCode);
        };
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);

        typeContributions.contributeJavaType(LocalDateTimeJavaType.INSTANCE);
        typeContributions.contributeJdbcType(LocalDateTimeJdbcType.INSTANCE);
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
                new CurrentFunction(
                        "current_time",
                        currentTime(),
                        localDateTimeType
                )
        );
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
    public int getInExpressionCountLimit() {
        return 100;
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
}
