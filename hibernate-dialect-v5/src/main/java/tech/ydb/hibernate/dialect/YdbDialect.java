package tech.ydb.hibernate.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.tool.schema.spi.Exporter;
import tech.ydb.hibernate.dialect.exporter.EmptyExporter;
import tech.ydb.hibernate.dialect.pagination.LimitOffsetLimitHandler;

import java.sql.Types;


/**
 * @author Kirill Kurdyukov
 */
public class YdbDialect extends Dialect {
    private static final int IN_EXPRESSION_COUNT_LIMIT = 10_000;

    private static final Exporter<ForeignKey> FOREIGN_KEY_EMPTY_EXPORTER = new EmptyExporter<>();
    private static final Exporter<Constraint> CONSTRAINT_EMPRY_EXPORTER = new EmptyExporter<>();

    public YdbDialect() {
        registerColumnType(Types.BIT, "Bool");
        registerColumnType(Types.BOOLEAN, "Bool");
        registerColumnType(Types.TINYINT, "Int8");
        registerColumnType(Types.SMALLINT, "Int16");
        registerColumnType(Types.INTEGER, "Int32");
        registerColumnType(Types.BIGINT, "Int64");
        registerColumnType(Types.FLOAT, "Float");
        registerColumnType(Types.DOUBLE, "Double");
        registerColumnType(Types.NUMERIC, "Decimal (22,9)");
        registerColumnType(Types.DECIMAL, "Decimal (22,9)");
        registerColumnType(Types.REAL, "Float");

        registerColumnType(Types.DATE, "Date");
        registerColumnType(Types.TIME, "Datetime");
        registerColumnType(Types.TIMESTAMP, "Timestamp");

        registerColumnType(Types.VARBINARY, "Bytes");
        registerColumnType(Types.LONGVARBINARY, "Bytes");
        registerColumnType(Types.BLOB, "Bytes");
        registerColumnType(Types.BINARY, "Bytes");

        registerColumnType(Types.CHAR, "Text");
        registerColumnType(Types.VARCHAR, "Text");
        registerColumnType(Types.LONGVARCHAR, "Text");
        registerColumnType(Types.CLOB, "Text");

        registerColumnType(Types.NCHAR, "Text");
        registerColumnType(Types.NVARCHAR, "Text");
        registerColumnType(Types.LONGNVARCHAR, "Text");
        registerColumnType(Types.NCLOB, "Text");

        registerColumnType(Types.TIME_WITH_TIMEZONE, "TzDateTime");
        registerColumnType(Types.TIMESTAMP_WITH_TIMEZONE, "TzTimestamp");
    }

    @Override
    public LimitHandler getLimitHandler() {
        return LimitOffsetLimitHandler.INSTANCE;
    }

    @Override
    public Exporter<ForeignKey> getForeignKeyExporter() {
        return FOREIGN_KEY_EMPTY_EXPORTER;
    }

    @Override
    public boolean supportsLockTimeouts() {
        return false;
    }

    @Override
    public boolean supportsOuterJoinForUpdate() {
        return false;
    }

    @Override
    public boolean supportsUnionAll() {
        return true;
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
    public String toBooleanValueString(boolean bool) {
        return String.valueOf(bool);
    }

    @Override
    public NameQualifierSupport getNameQualifierSupport() {
        return NameQualifierSupport.NONE;
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
    public String getAddColumnString() {
        return "add column";
    }

    @Override
    public boolean supportsSelectAliasInGroupByClause() {
        return true;
    }

    @Override
    public boolean supportsPartitionBy() {
        return true;
    }

    @Override
    public boolean supportsValuesList() {
        return true;
    }

    @Override
    public boolean supportsExistsInSelect() {
        return false;
    }

    @Override
    public int getInExpressionCountLimit() {
        return IN_EXPRESSION_COUNT_LIMIT;
    }

    @Override
    public Exporter<Constraint> getUniqueKeyExporter() {
        return CONSTRAINT_EMPRY_EXPORTER;
    }
}
