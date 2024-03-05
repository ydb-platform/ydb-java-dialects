package tech.ydb.flywaydb.database;

import java.sql.SQLException;
import org.flywaydb.core.api.FlywayException;
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
    protected boolean doEmpty() throws SQLException {
        return doAllTables().length == 0;
    }

    @Override
    protected void doCreate() throws SQLException {
        throw new FlywayException("YDB doesn't support SCHEMA");
    }

    @Override
    protected void doDrop() throws SQLException {
        throw new FlywayException("YDB doesn't support SCHEMA");
    }

    @Override
    protected void doClean() throws SQLException {

    }

    @Override
    protected YdbTable[] doAllTables() throws SQLException {


        jdbcTemplate.getConnection().getMetaData()
                .getTables(null, null, null, null)

        return new YdbTable[0];
    }

    @Override
    public YdbTable getTable(String tableName) {
        return new YdbTable(jdbcTemplate, database, this, tableName);
    }
}
