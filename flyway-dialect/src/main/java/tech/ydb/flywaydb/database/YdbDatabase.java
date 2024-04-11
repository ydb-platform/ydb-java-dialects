package tech.ydb.flywaydb.database;

import java.sql.Connection;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.database.base.Table;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabase extends Database<YdbConnection> {

    public YdbDatabase(
            Configuration configuration,
            JdbcConnectionFactory jdbcConnectionFactory,
            StatementInterceptor statementInterceptor
    ) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    protected YdbConnection doGetConnection(Connection connection) {
        return new YdbConnection(this, connection);
    }

    @Override
    public void ensureSupported(Configuration configuration) {
    }

    @Override
    public boolean supportsDdlTransactions() {
        return false;
    }

    @Override
    public String getBooleanTrue() {
        return "TRUE";
    }

    @Override
    public String getBooleanFalse() {
        return "FALSE";
    }

    @Override
    public boolean catalogIsSchema() {
        return false;
    }

    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        return "CREATE TABLE " + doQuote(table.getName()) + " (\n" +
                "    installed_rank INT32 NOT NULL,\n" +
                "    version TEXT,\n" +
                "    description TEXT,\n" +
                "    type TEXT,\n" +
                "    script TEXT,\n" +
                "    checksum INT32,\n" +
                "    installed_by TEXT,\n" +
                "    installed_on DATETIME,\n" +
                "    execution_time INT32,\n" +
                "    success BOOL,\n" +
                "    PRIMARY KEY (installed_rank)" +
                ");\n" +
                (baseline ? getBaselineStatement(table) : "");
    }

    @Override
    public String getSelectStatement(Table table) {
        return "SELECT " + quote("installed_rank")
                + "," + quote("version")
                + "," + quote("description")
                + "," + quote("type")
                + "," + quote("script")
                + "," + quote("checksum")
                + "," + quote("installed_on")
                + "," + quote("installed_by")
                + "," + quote("execution_time")
                + "," + quote("success")
                + " FROM " + quote(table.getName())
                + " WHERE " + quote("installed_rank") + " > ?"
                + " ORDER BY " + quote("installed_rank");
    }

    @Override
    public String getInsertStatement(Table table) {
        return "INSERT INTO " + quote(table.getName())
                + " (" + quote("installed_rank")
                + ", " + quote("version")
                + ", " + quote("description")
                + ", " + quote("type")
                + ", " + quote("script")
                + ", " + quote("checksum")
                + ", " + quote("installed_by")
                + ", " + quote("execution_time")
                + ", " + quote("success")
                + ", " + quote("installed_on")
                + ")"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CurrentUtcDatetime())";
    }

    @Override
    protected String getOpenQuote() {
        return "`";
    }

    @Override
    protected String getCloseQuote() {
        return "`";
    }

    @Override
    protected String getEscapedQuote() {
        return "\\";
    }
}
