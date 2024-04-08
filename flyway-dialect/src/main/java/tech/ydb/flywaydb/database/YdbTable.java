package tech.ydb.flywaydb.database;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.internal.database.base.Table;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;
import org.flywaydb.core.internal.jdbc.Results;

/**
 * @author Kirill Kurdyukov
 */
public class YdbTable extends Table<YdbDatabase, YdbSchema> {

    private static final Duration WAIT_LOCK_TIMEOUT = Duration.ofMinutes(2);
    private static final int RELEASE_MAX_ATTEMPT = 10;

    private final String tableLockId = UUID.randomUUID() + "-flyway-lock-id";
    private final Random random = new Random();

    /**
     * @param jdbcTemplate The JDBC template for communicating with the YDB.
     * @param database     The database-specific support.
     * @param schema       The schema this table lives in.
     * @param name         The name of the table.
     */
    public YdbTable(JdbcTemplate jdbcTemplate, YdbDatabase database, YdbSchema schema, String name) {
        super(jdbcTemplate, database, schema, name);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return exists(null, schema, name);
    }

    @Override
    protected void doLock() {
        Instant startLock = Instant.now();

        do {
            if (insertLockingRow()) {
                return;
            }

            try {
                Thread.sleep(100 + random.nextInt(1000)); // pause 0.1s .. 1.1s
            } catch (InterruptedException ignored) {
            }
        } while (startLock.minus(WAIT_LOCK_TIMEOUT).isAfter(Instant.now()));

        throw new FlywayException("Unable to obtain table lock - another Flyway instance may be running");
    }

    @Override
    protected void doUnlock() {
        for (int attempt = 0; attempt < RELEASE_MAX_ATTEMPT; attempt++) {
            SQLException sqlException = jdbcTemplate
                    .executeStatement("DELETE FROM " + this + " WHERE installed_rank = -100")
                    .getException();

            if (sqlException == null) {
                return;
            }

            if (attempt == RELEASE_MAX_ATTEMPT - 1) {
                throw new FlywayException(sqlException);
            }
        }
    }

    @Override
    protected void doDrop() throws SQLException {
        jdbcTemplate.execute("DROP TABLE " + database.doQuote(name));
    }

    /**
     * Insert the locking row - the primary keys of installed_rank will prevent us having two.
     *
     * @return true if no errors.
     */
    private boolean insertLockingRow() {
        Results results = jdbcTemplate.executeStatement(
                "INSERT INTO " + this +
                        "(installed_rank, version, description, type, script, " +
                        "checksum, installed_by, execution_time, success, installed_on) " +
                        "VALUES (-100, '" + tableLockId + "', 'flyway-lock', " +
                        "'', '', 0, '', 0, TRUE, CurrentUtcDatetime())"
        );

        // Succeeded if no errors.
        return results.getException() == null;
    }

    @Override
    public String toString() {
        return database.doQuote(name);
    }
}
