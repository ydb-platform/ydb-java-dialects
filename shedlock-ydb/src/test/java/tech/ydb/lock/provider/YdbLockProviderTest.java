package tech.ydb.lock.provider;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
    private YdbCoordinationServiceLockProvider lockProvider;

    @Test
    public void integrationTest() throws ExecutionException, InterruptedException {
        var executorServer = Executors.newFixedThreadPool(10);
        var atomicInt = new AtomicInteger();
        var locked = new AtomicBoolean();

        for (int i = 0; i < 10; i++) {
            executorServer.submit(() -> {
                Optional<SimpleLock> optinal = Optional.empty();

                while (optinal.isEmpty()) {
                    optinal = lockProvider.lock(
                            new LockConfiguration(Instant.now(), "semaphore", Duration.ZERO, Duration.ZERO));

                    optinal.ifPresent(simpleLock -> {
                        if (locked.get()) {
                            throw new RuntimeException();
                        }
                        locked.set(true);

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        atomicInt.addAndGet(50);
                        locked.set(false);
                        simpleLock.unlock();
                    });
                }
            }).get();
        }

        Assertions.assertEquals(500, atomicInt.get());
    }
}
