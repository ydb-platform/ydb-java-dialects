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
        return "CREATE TABLE " + table + " (\n" +
                "    installed_rank INT32 NOT NULL,\n" +
                "    version TEXT,\n" +
                "    description TEXT,\n" +
                "    script TEXT,\n" +
                "    checksum INT32,\n" +
                "    installed_by TEXT,\n" +
                "    installed_on TIMESTAMP,\n" +
                "    execution_time INT32,\n" +
                "    success BOOL,\n" +
                "    PRIMARY KEY (installed_rank)" +
                ")";
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
