package tech.ydb.lock.provider;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.support.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SemaphoreLease;
import tech.ydb.core.Result;
import tech.ydb.jdbc.YdbConnection;

/**
 * @author Kirill Kurdyukov
 */
public class YdbCoordinationServiceLockProvider implements LockProvider {
    private static final Logger logger = LoggerFactory.getLogger(YdbCoordinationServiceLockProvider.class);
    private static final String YDB_LOCK_NODE_NAME = "shared-lock-ydb";
    private static final int ATTEMPT_CREATE_NODE = 10;
    private static final String INSTANCE_INFO =
            "{Hostname=" + Utils.getHostname() + ", " + "Current PID=" + ProcessHandle.current().pid() + "}";
    private static final byte[] INSTANCE_INFO_BYTES = INSTANCE_INFO.getBytes(StandardCharsets.UTF_8);

    private final YdbConnection ydbConnection;
    private final CoordinationClient coordinationClient;

    private volatile CoordinationSession coordinationSession;

    public YdbCoordinationServiceLockProvider(YdbConnection ydbConnection) {
        this.ydbConnection = ydbConnection;
        this.coordinationClient = CoordinationClient.newClient(ydbConnection.getCtx().getGrpcTransport());
    }

    @PostConstruct
    public void init() {
        for (int i = 0; i < ATTEMPT_CREATE_NODE; i++) {
            var status = coordinationClient.createNode(YDB_LOCK_NODE_NAME).join();

            if (status.isSuccess()) {
                coordinationSession = coordinationClient.createSession(YDB_LOCK_NODE_NAME);

                var statusCS = coordinationSession.connect().join();

                if (statusCS.isSuccess()) {
                    logger.info("Created coordination node session [{}]", coordinationSession);

                    return;
                }
                if (i == ATTEMPT_CREATE_NODE - 1) {
                    statusCS.expectSuccess("Failed creating coordination node session");
                }
            }

            if (i == ATTEMPT_CREATE_NODE - 1) {
                status.expectSuccess("Failed created coordination service node: " + YDB_LOCK_NODE_NAME);
            }
        }
    }

    @Override
    public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
        logger.info("Instance[{}] is trying to become a leader...", INSTANCE_INFO);

        Result<SemaphoreLease> semaphoreLease = coordinationSession.acquireEphemeralSemaphore(
                lockConfiguration.getName(),
                true,
                INSTANCE_INFO_BYTES,
                lockConfiguration.getLockAtMostFor()
        ).join();

        if (semaphoreLease.isSuccess()) {
            logger.info("Instance[{}] acquired semaphore[SemaphoreName={}]", INSTANCE_INFO,
                    semaphoreLease.getValue().getSemaphoreName());

            return Optional.of(new YdbSimpleLock(semaphoreLease.getValue()));
        } else {
            logger.info("Instance[{}] did not acquire semaphore", INSTANCE_INFO);

            return Optional.empty();
        }
    }

    private record YdbSimpleLock(SemaphoreLease semaphoreLease) implements SimpleLock {
        @Override
        public void unlock() {
            logger.info("Instance[{}] released semaphore[SemaphoreName={}]", INSTANCE_INFO, semaphoreLease.getSemaphoreName());

            semaphoreLease.release().join();
        }
    }

    @PreDestroy
    private void close() throws SQLException {
        // closing coordination session
        coordinationSession.close();

        ydbConnection.close();
    }
}
