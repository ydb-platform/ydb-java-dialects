package tech.ydb.flywaydb.database;

import org.flywaydb.core.internal.database.base.Connection;

/**
 * @author Kirill Kurdyukov
 */
public class YdbConnection extends Connection<YdbDatabase> {
    protected YdbConnection(YdbDatabase database, java.sql.Connection connection) {
        super(database, connection);
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() {
        return null;
    }

    @Override
    public YdbSchema getSchema(String s) {
        return new YdbSchema(jdbcTemplate, database, s);
    }
}
