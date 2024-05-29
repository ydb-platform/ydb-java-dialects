package tech.ydb.flywaydb.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
        return true; // Dummy schema exists
    }

    @Override
    protected boolean doEmpty() throws SQLException {
        return doAllTables().length == 0;
    }

    @Override
    protected void doCreate() {
        // Do nothing, YDB doesn't support schemas
    }

    @Override
    protected void doDrop() {
        // Do nothing, YDB doesn't support schemas
    }

    @Override
    protected void doClean() throws SQLException {
        List<String> schemaTables = schemaTables();

        if (schemaTables.isEmpty()) {
            return;
        }

        jdbcTemplate.executeStatement(
                schemaTables.stream()
                        .map(table -> "DROP TABLE " + table)
                        .collect(Collectors.joining("; "))
        );
    }

    @Override
    protected YdbTable[] doAllTables() throws SQLException {
        return schemaTables().stream()
                .map(table -> new YdbTable(jdbcTemplate, database, this, name))
                .toArray(YdbTable[]::new);
    }

    @Override
    public YdbTable getTable(String tableName) {
        return new YdbTable(jdbcTemplate, database, this, tableName);
    }

    @Override
    public String toString() {
        return "ydb_schema";
    }

    private List<String> schemaTables() throws SQLException {
        ResultSet rs = jdbcTemplate.getConnection().getMetaData()
                .getTables(null, name, null, new String[]{"TABLE"});

        List<String> tables = new ArrayList<>();

        while (rs.next()) {
            tables.add(database.quote(rs.getString("TABLE_NAME")));
        }

        return tables;
    }
}
