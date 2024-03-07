package tech.ydb.flywaydb.database;

import org.flywaydb.core.internal.database.base.Schema;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;

/**
 * @author Kirill Kurdyukov
 */
public class YdbSchema extends Schema<YdbDatabase, YdbTable> {

    /**
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    public YdbSchema(JdbcTemplate jdbcTemplate, YdbDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    @Override
    protected boolean doExists() {
        return false;
    }

    @Override
    protected boolean doEmpty() {
        return doAllTables().length == 0;
    }

    @Override
    protected void doCreate() {
    }

    @Override
    protected void doDrop() {
    }

    @Override
    protected void doClean() {

    }

    @Override
    protected YdbTable[] doAllTables() {
        return new YdbTable[0];
    }

    @Override
    public YdbTable getTable(String tableName) {
        return new YdbTable(jdbcTemplate, database, this, tableName);
    }
}
