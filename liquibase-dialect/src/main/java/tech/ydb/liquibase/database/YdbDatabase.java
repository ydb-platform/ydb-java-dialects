package tech.ydb.liquibase.database;

import java.sql.Date;
import java.sql.Timestamp;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabase extends AbstractJdbcDatabase {

    private final static String DATABASE_PRODUCT_NAME = "YDB";
    private final static String DATABASE_QUOTING_CHARACTER = "`";
    private final static int DATABASE_DEFAULT_PORT = 2136;
    private final static String DRIVER_NAME = "tech.ydb.jdbc.YdbDriver";

    public YdbDatabase() {
        super.setCurrentDateTimeFunction("CurrentUtcDatetime()");
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return DATABASE_PRODUCT_NAME;
    }

    @Override
    public boolean isCorrectDatabaseImplementation(DatabaseConnection databaseConnection) throws DatabaseException {
        return DATABASE_PRODUCT_NAME.equalsIgnoreCase(databaseConnection.getDatabaseProductName());
    }

    @Override
    public String getDefaultDriver(String url) {
        if (url.startsWith("jdbc:ydb")) {
            return DRIVER_NAME;
        }

        return null;
    }

    @Override
    public String getShortName() {
        return DATABASE_PRODUCT_NAME.toLowerCase();
    }

    @Override
    public Integer getDefaultPort() {
        return DATABASE_DEFAULT_PORT;
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public String escapeStringForDatabase(String string) {
        if (string == null) {
            return null;
        }

        return string.replace("\\", "\\\\").replace("'", "\\'");
    }

    @Override
    public boolean requiresUsername() {
        return false;
    }

    @Override
    public String getDateLiteral(Date date) {
        return "DATE(" + super.getDateLiteral(date) + ")";
    }

    @Override
    public String getDateTimeLiteral(Timestamp date) {
        return "CAST(" + date.toInstant().getEpochSecond() + " AS DATETIME)";
    }

    @Override
    public boolean supportsBatchUpdates() {
        return true;
    }

    @Override
    public boolean requiresPassword() {
        return false;
    }

    @Override
    protected String getQuotingStartCharacter() {
        return DATABASE_QUOTING_CHARACTER;
    }

    @Override
    protected String getQuotingEndCharacter() {
        return DATABASE_QUOTING_CHARACTER;
    }

    @Override
    public boolean supportsCatalogs() {
        return false;
    }

    @Override
    public boolean supportsSchemas() {
        return false;
    }

    @Override
    public boolean supportsInitiallyDeferrableColumns() {
        return false;
    }

    @Override
    public boolean supportsTablespaces() {
        return false;
    }

    /**
     * YDB does not support foreign key
     */
    @Override
    public boolean supportsForeignKeyDisable() {
        return true;
    }

    /**
     * YDB does not support sequences (yet)
     */
    @Override
    public boolean supportsSequences() {
        return false;
    }

    /**
     * YDB does not support auto increment (yet)
     */
    @Override
    public boolean supportsAutoIncrement() {
        return false;
    }

    @Override
    public boolean supportsDDLInTransaction() {
        return false;
    }

    @Override
    public boolean supportsPrimaryKeyNames() {
        return false;
    }
}
