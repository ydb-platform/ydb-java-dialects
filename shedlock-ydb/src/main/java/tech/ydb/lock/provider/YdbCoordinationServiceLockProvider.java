package tech.ydb.lock.provider;

import java.sql.SQLException;
import java.util.Optional;
import javax.annotation.PreDestroy;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.jdbc.YdbConnection;

/**
 * @author Kirill Kurdyukov
 */
public class YdbCoordinationServiceLockProvider implements LockProvider {
    private static final Logger logger = LoggerFactory.getLogger(YdbCoordinationServiceLockProvider.class);
    private static final String YDB_LOCK_NODE_NAME = "shared-lock-ydb";
    private static final int ATTEMPT_CREATE_NODE = 10;

    private final YdbConnection ydbConnection;
    private final CoordinationClient coordinationClient;

    public YdbCoordinationServiceLockProvider(YdbConnection ydbConnection) {
        this.ydbConnection = ydbConnection;
        this.coordinationClient = CoordinationClient.newClient(ydbConnection.getCtx().getGrpcTransport());
    }

    public void init() {
        for (int i = 0; i < ATTEMPT_CREATE_NODE; i++) {
            var status = coordinationClient.createNode(YDB_LOCK_NODE_NAME).join();

            if (status.isSuccess()) {
                return;
            }

            if (i == ATTEMPT_CREATE_NODE - 1) {
                status.expectSuccess("Failed created coordination service node: " + YDB_LOCK_NODE_NAME);
            }
        }
    }

    @Override
    public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
        var coordinationSession = coordinationClient.createSession(YDB_LOCK_NODE_NAME);

        coordinationSession.connect().join()
                .expectSuccess("Failed creating coordination node session");

        logger.debug("Created coordination node session");

        var semaphoreLease = coordinationSession.acquireEphemeralSemaphore(lockConfiguration.getName(), true,
                lockConfiguration.getLockAtMostFor()).join();

        if (semaphoreLease.isSuccess()) {
            logger.debug("Semaphore acquired");

            System.out.println(semaphoreLease.getStatus());
            return Optional.of(new YdbSimpleLock(semaphoreLease.getValue()));
        } else {
            logger.debug("Semaphore is not acquired");
            return Optional.empty();
        }
    }

    private record YdbSimpleLock(SemaphoreLease semaphoreLease) implements SimpleLock {
        @Override
        public void unlock() {
            semaphoreLease.release().join();
        }
    }

    @PreDestroy
    private void close() throws SQLException {
        ydbConnection.close();
    }
}
