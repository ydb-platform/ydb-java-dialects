package tech.ydb.lock.provider;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
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
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.core.Result;
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
        var now = Instant.now();

        String instanceInfo = "Hostname=" + Utils.getHostname() + ", " +
                "Current PID=" + ProcessHandle.current().pid() + ", " +
                "CreatedAt=" + now;

        logger.info("Instance[{}] is trying to become a leader...", instanceInfo);

        var describeResult = coordinationSession.describeSemaphore(
                lockConfiguration.getName(),
                DescribeSemaphoreMode.WITH_OWNERS
        ).join();

        if (describeResult.isSuccess()) {
            var describe = describeResult.getValue();
            var describePayload = new String(describe.getData(), StandardCharsets.UTF_8);

            logger.debug("Received DescribeSemaphore[Name={}, Data={}]", describe.getName(), describePayload);

            Instant createdLeaderTimestampUTC = Instant.parse(describePayload.split(",")[2].split("=")[1]);

            if (now.isAfter(createdLeaderTimestampUTC.plus(lockConfiguration.getLockAtMostFor()))) {
                var deleteResult = coordinationSession.deleteSemaphore(describe.getName(), true).join();
                logger.debug("Delete semaphore[Name={}] result: {}", describe.getName(), deleteResult);
            }
        } else {
            // no success, ephemeral semaphore is not created

            logger.debug("Semaphore[Name={}] not found", lockConfiguration.getName());
        }

        Result<SemaphoreLease> semaphoreLease = coordinationSession.acquireEphemeralSemaphore(
                lockConfiguration.getName(),
                true,
                instanceInfo.getBytes(StandardCharsets.UTF_8),
                lockConfiguration.getLockAtMostFor()
        ).join();

        if (semaphoreLease.isSuccess()) {
            logger.info("Instance[{}] acquired semaphore[SemaphoreName={}]", instanceInfo,
                    semaphoreLease.getValue().getSemaphoreName());

            return Optional.of(new YdbSimpleLock(semaphoreLease.getValue(), instanceInfo));
        } else {
            logger.info("Instance[{}] did not acquire semaphore", instanceInfo);

            return Optional.empty();
        }
    }

    private record YdbSimpleLock(SemaphoreLease semaphoreLease, String metaInfo) implements SimpleLock {
        @Override
        public void unlock() {
            logger.info("Instance[{}] released semaphore[SemaphoreName={}]", metaInfo, semaphoreLease.getSemaphoreName());

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
