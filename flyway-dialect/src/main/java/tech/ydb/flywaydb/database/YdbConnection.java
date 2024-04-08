package tech.ydb.flywaydb.database;

import org.flywaydb.core.internal.database.base.Connection;

/**
 * @author Kirill Kurdyukov
 */
public class YdbConnection extends Connection<YdbDatabase> {

    private static final String DUMMY_SCHEMA_NAME = "";

    protected YdbConnection(YdbDatabase database, java.sql.Connection connection) {
        super(database, connection);

        this.jdbcTemplate = new YdbJdbcTemplate(connection, database.getDatabaseType());
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() {
        return null; // schema isn't supported
    }

    @Override
    public YdbSchema getSchema(String name) {
        return new YdbSchema(jdbcTemplate, database, DUMMY_SCHEMA_NAME);
    }
}
