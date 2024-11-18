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

                var selectPS = connection.prepareStatement("SELECT lock_until, locked_by FROM shedlock " +
                        "WHERE name = ? AND lock_until > CurrentUtcTimestamp() + ?");

                selectPS.setString(1, lockConfiguration.getName());
                selectPS.setObject(2, lockConfiguration.getLockAtMostFor());

                var haveLeader = false;
                try (var rs = selectPS.executeQuery()) {
                    haveLeader = rs.next();
                }

                if (haveLeader) {
                    return Optional.empty();
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
            LOGGER.debug(String.format("Instance[{%s}] acquire lock is failed", LOCKED_BY), e);

            return Optional.empty();
        }
    }

    private record YdbJDBCLock(String name, DataSource dataSource) implements SimpleLock {
        @Override
        public void unlock() {
            try (var connection = dataSource.getConnection()) {
                var ps = connection.prepareStatement(
                        "UPDATE shedlock SET lock_until = CurrentUtcTimestamp() WHERE name = ?");
                ps.setObject(1, name);
                ps.execute();
            } catch (SQLException e) {
                LOGGER.error(String.format("Instance[{%s}] release lock is failed", LOCKED_BY), e);

                throw new RuntimeException(e);
            }
        }
    }
}
