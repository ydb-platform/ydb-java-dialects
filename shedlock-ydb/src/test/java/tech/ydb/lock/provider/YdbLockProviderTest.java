package tech.ydb.lock.provider;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tech.ydb.test.junit5.YdbHelperExtension;

/**
 * @author Kirill Kurdyukov
 */
@SpringBootTest(classes = TestApp.class)
public class YdbLockProviderTest {
    private static final Logger logger = LoggerFactory.getLogger(YdbLockProviderTest.class);

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @DynamicPropertySource
    private static void propertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", YdbLockProviderTest::jdbcUrl);
    }

    private static String jdbcUrl() {
        StringBuilder jdbc = new StringBuilder("jdbc:ydb:")
                .append(ydb.useTls() ? "grpcs://" : "grpc://")
                .append(ydb.endpoint())
                .append(ydb.database());

        if (ydb.authToken() != null) {
            jdbc.append("?").append("token=").append(ydb.authToken());
        }

        return jdbc.toString();
    }

    @Autowired
    private YdbJDBCLockProvider lockProvider;

    @Autowired
    private DataSource dataSource;

    @Test
    public void integrationTest() throws ExecutionException, InterruptedException, SQLException {
        try (var connection = dataSource.getConnection()) {
            var statement = connection.createStatement();
            statement.execute(
                    "CREATE TABLE shedlock(" +
                            "name TEXT NOT NULL, " +
                            "lock_until TIMESTAMP NOT NULL," +
                            "locked_at TIMESTAMP NOT NULL," +
                            "locked_by TEXT NOT NULL, " +
                            "PRIMARY KEY (name)" +
                            ");");
        }

        var lockFutures = new ArrayList<Future<?>>();
        var executorServer = Executors.newFixedThreadPool(10);
        var atomicInt = new AtomicInteger();
        var locked = new AtomicBoolean();

        for (int i = 0; i < 100; i++) {
            lockFutures.add(executorServer.submit(() -> {
                Optional<SimpleLock> optinal = Optional.empty();

                while (optinal.isEmpty()) {
                    optinal = lockProvider.lock(new LockConfiguration(
                            Instant.now(), "semaphore", Duration.ofSeconds(100), Duration.ZERO));

                    optinal.ifPresent(simpleLock -> {
                        if (!locked.compareAndSet(false, true)) {
                            logger.error("Failed test! System has two leaders");

                            throw new RuntimeException();
                        }

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        atomicInt.addAndGet(50);
                        locked.set(false);
                        logger.info("Leader does UNLOCK!");
                        simpleLock.unlock();
                    });

                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(3_000));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }));
        }

        for (var future : lockFutures) {
            future.get();
        }

        Assertions.assertEquals(5000, atomicInt.get());
    }
}
