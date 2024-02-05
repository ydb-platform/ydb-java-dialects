package tech.ydb.liquibase.database;

import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import tech.ydb.jdbc.YdbDriver;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabase extends AbstractJdbcDatabase {

    private final static String DATABASE_PRODUCT_NAME = "YDB";
    private final static String DATABASE_QUOTING_CHARACTER = "`";
    private final static int DATABASE_DEFAULT_PORT = 2136;

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
            return YdbDriver.class.getName();
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
    public boolean requiresUsername() {
        return false;
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
     * YDB don't support foreign key
     */
    @Override
    public boolean supportsForeignKeyDisable() {
        return true;
    }

    /**
     * YDB don't support sequences
     */
    @Override
    public boolean supportsSequences() {
        return false;
    }

    /**
     * YDB don't support auto increment
     */
    @Override
    public boolean supportsAutoIncrement() {
        return false;
    }
}
