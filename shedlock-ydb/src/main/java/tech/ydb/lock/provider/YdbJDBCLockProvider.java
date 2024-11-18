package tech.ydb.lock.provider;

import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.support.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kirill Kurdyukov
 */
public class YdbJDBCLockProvider implements LockProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(YdbJDBCLockProvider.class);
    private static final String LOCKED_BY = "Hostname=" + Utils.getHostname() + ", " +
            "Current PID=" + ProcessHandle.current().pid();

    private final DataSource dataSource;

    public YdbJDBCLockProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
        try (var connection = dataSource.getConnection()) {
            var autoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);

                var selectPS = connection.prepareStatement("SELECT locked_by, lock_until FROM shedlock " +
                        "WHERE name = ? AND lock_until > CurrentUtcTimestamp()");

                selectPS.setString(1, lockConfiguration.getName());

                try (var rs = selectPS.executeQuery()) {
                    if (rs.next()) {
                        LOGGER.debug("Instance[{}] acquire lock is failed. Leader is {}, lock_until = {}",
                                LOCKED_BY, rs.getString(1), rs.getString(2));
                        return Optional.empty();
                    }
                }

                var upsertPS = connection.prepareStatement("" +
                        "UPSERT INTO shedlock(name, lock_until, locked_at, locked_by) " +
                        "VALUES (?, Unwrap(CurrentUtcTimestamp() + ?), CurrentUtcTimestamp(), ?)"
                );

                upsertPS.setObject(1, lockConfiguration.getName());
                upsertPS.setObject(2, lockConfiguration.getLockAtMostFor());
                upsertPS.setObject(3, LOCKED_BY);
                upsertPS.execute();

                connection.commit();

                LOGGER.debug("Instance[{}] is leader", LOCKED_BY);

                return Optional.of(new YdbJDBCLock(lockConfiguration.getName(), dataSource));
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            LOGGER.debug("Instance[{}] acquire lock is failed", LOCKED_BY);

            return Optional.empty();
        }
    }

    private record YdbJDBCLock(String name, DataSource dataSource) implements SimpleLock {
        private static final int ATTEMPT_RELEASE_LOCK = 10;

        @Override
        public void unlock() {
            for (int i = 0; i < ATTEMPT_RELEASE_LOCK; i++) {
                try {
                    doUnlock();

                    return;
                } catch (SQLException e) {
                    if (i == ATTEMPT_RELEASE_LOCK - 1) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        private void doUnlock() throws SQLException {
            try (var connection = dataSource.getConnection()) {
                var autoCommit = connection.getAutoCommit();

                try {
                    connection.setAutoCommit(true);
                    var ps = connection.prepareStatement(
                            "UPDATE shedlock SET lock_until = CurrentUtcTimestamp() WHERE name = ? and locked_by = ?");
                    ps.setString(1, name);
                    ps.setString(2, LOCKED_BY);
                    ps.execute();
                } finally {
                    connection.setAutoCommit(autoCommit);
                }
            } catch (SQLException e) {
                LOGGER.debug(String.format("Instance[{%s}] release lock is failed", LOCKED_BY), e);

                throw e;
            }
        }
    }
}
