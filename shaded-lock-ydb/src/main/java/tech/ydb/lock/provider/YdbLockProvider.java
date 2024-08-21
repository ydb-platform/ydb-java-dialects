package tech.ydb.lock.provider;

import java.util.Optional;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.SemaphoreLease;

/**
 * @author Kirill Kurdyukov
 */
public class YdbLockProvider implements LockProvider {
    private static final Logger logger = LoggerFactory.getLogger(YdbLockProvider.class);
    private static final String YDB_LOCK_NODE_NAME = "shared-lock-ydb";
    private static final int ATTEMPT_CREATE_NODE = 10;

    private final CoordinationClient coordinationClient;
    private final YdbLockProperties ydbLockProperties;

    public YdbLockProvider(CoordinationClient coordinationClient, YdbLockProperties ydbLockProperties) {
        this.coordinationClient = coordinationClient;
        this.ydbLockProperties = ydbLockProperties;
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

        coordinationSession.createSemaphore(lockConfiguration.getName(), 1).join()
                .expectSuccess("Failed creating semaphore[" + lockConfiguration.getName() +
                        "], coordination node[" + YDB_LOCK_NODE_NAME + "]");

        logger.debug("Created semaphore[" + lockConfiguration.getName() + "]");

        var semaphoreLease = coordinationSession.acquireSemaphore(lockConfiguration.getName(), 1,
                ydbLockProperties.acquireSemaphoreTimeout).join().getValue();

        logger.debug("Semaphore acquired");

        return Optional.of(new YdbSimpleLock(semaphoreLease));
    }

    private record YdbSimpleLock(SemaphoreLease semaphoreLease) implements SimpleLock {
        @Override
            public void unlock() {
                semaphoreLease.release().join();
            }
        }
}
