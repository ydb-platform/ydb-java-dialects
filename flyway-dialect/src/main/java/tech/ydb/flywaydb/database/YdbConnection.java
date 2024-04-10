package tech.ydb.flywaydb.database;

import org.flywaydb.core.internal.database.base.Connection;

/**
 * @author Kirill Kurdyukov
 */
public class YdbConnection extends Connection<YdbDatabase> {

    private static final String YDB_SCHEMA_NAME = "";

    protected YdbConnection(YdbDatabase database, java.sql.Connection connection) {
        super(database, connection);

        this.jdbcTemplate = new YdbJdbcTemplate(connection, database.getDatabaseType());
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() {
        return YDB_SCHEMA_NAME; // schema isn't supported
    }

    @Override
    public YdbSchema getSchema(String name) {
        return new YdbSchema(jdbcTemplate, database, YDB_SCHEMA_NAME);
    }
}
