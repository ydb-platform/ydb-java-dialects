package tech.ydb.flywaydb.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import org.flywaydb.core.internal.database.base.Table;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.jdbc.YdbConnection;

/**
 * @author Kirill Kurdyukov
 */
public class YdbTable extends Table<YdbDatabase, YdbSchema> {

    private static final String COORDINATION_NODE_NAME = "flyway-coordination-node";
    private static final int LOCK_TIMEOUT_MS = 60_000;

    private SemaphoreLease semaphoreLease;

    /**
     * @param jdbcTemplate The JDBC template for communicating with the DB.
     * @param database     The database-specific support.
     * @param schema       The schema this table lives in.
     * @param name         The name of the table.
     */
    public YdbTable(JdbcTemplate jdbcTemplate, YdbDatabase database, YdbSchema schema, String name) {
        super(jdbcTemplate, database, schema, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        ResultSet resultSet = jdbcTemplate.getConnection()
                .unwrap(YdbConnection.class)
                .getMetaData()
                .getTables(null, null, getName(), null);

        return resultSet.next();
    }

    @Override
    protected void doLock() throws SQLException {
        CoordinationClient coordinationClient = CoordinationClient.newClient(
                jdbcTemplate.getConnection().unwrap(YdbConnection.class)
                        .getCtx().getGrpcTransport()
        );

        coordinationClient.createNode(COORDINATION_NODE_NAME).join(); // TODO Retry policy

        CoordinationSession session = coordinationClient.createSession(COORDINATION_NODE_NAME);
        session.connect().join();

        semaphoreLease = session
                .acquireEphemeralSemaphore(getName(), true, Duration.ofMillis(LOCK_TIMEOUT_MS))
                .join()
                .getValue();
    }

    @Override
    protected void doUnlock() {
        semaphoreLease.release();
        semaphoreLease.getSession().close();
    }

    @Override
    protected void doDrop() throws SQLException {
        jdbcTemplate.execute("DROP TABLE " + database.doQuote(name));
    }
}
