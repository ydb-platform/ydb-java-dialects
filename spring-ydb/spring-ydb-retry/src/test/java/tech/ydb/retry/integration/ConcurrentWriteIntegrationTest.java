package tech.ydb.retry.integration;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tech.ydb.core.StatusCode;
import tech.ydb.retry.integration.app.User;
import tech.ydb.retry.integration.app.UserApplication;
import tech.ydb.retry.integration.app.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = UserApplication.class)
@ActiveProfiles({"enabled", "ydb"})
class ConcurrentWriteIntegrationTest extends YdbDockerTest {

    @Autowired
    private UserService userService;

    @BeforeEach
    void cleanUp() {
        DeterministicErrorChannel.configure();
        DeterministicErrorChannel.resetCounters();
        userService.deleteAll();
    }

    @Test
    void shouldInsertConcurrently() throws Exception {
        ConcurrentRunner.with(10)
                .execute(idx -> userService.save(
                        new User(1000L + idx, "user" + idx, "first" + idx, "last" + idx)))
                .awaitCompletion(30, TimeUnit.SECONDS)
                .assertAllSucceeded();

        for (int i = 0; i < 10; i++) {
            assertNotNull(userService.findById(1000L + i));
        }
    }

    @Test
    void shouldRetryOnConcurrentChannelErrors() throws Exception {
        DeterministicErrorChannel.configure().onError("commitTransaction", 1, StatusCode.ABORTED);

        ConcurrentRunner.with(5)
                .execute(idx -> userService.save(
                        new User(200L + idx, "user" + idx, "first" + idx, "last" + idx)))
                .awaitCompletion(30, TimeUnit.SECONDS)
                .assertAllSucceeded();
    }

    @Test
    void shouldResolveConcurrentUpdateConflictsViaRetry() throws Exception {
        userService.saveRaw(new User(1L, "user", "original", "original"));

        ConcurrentRunner.with(5)
                .execute(idx -> userService.updateFirstname(1L, "new" + idx))
                .awaitCompletion(60, TimeUnit.SECONDS)
                .assertAllSucceeded();

        String firstname = userService.findById(1L).getFirstname();
        assertTrue(firstname.startsWith("new"));
        assertTrue(DeterministicErrorChannel.getCallCount("commitTransaction") > 5);
    }

    @Test
    void shouldInsertConcurrentlyWithRetryErrors() throws Exception {
        DeterministicErrorChannel.configure()
                .onError("executeQuery", 1, StatusCode.ABORTED)
                .onError("executeQuery", 2, StatusCode.BAD_SESSION);

        ConcurrentRunner.with(3)
                .execute(idx -> userService.save(
                        new User(300L + idx, "user" + idx, "first" + idx, "last" + idx)))
                .awaitCompletion(30, TimeUnit.SECONDS)
                .assertAllSucceeded();

        assertEquals(5, DeterministicErrorChannel.getCallCount("executeQuery"));
    }
}
