package tech.ydb.retry.integration;

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
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = UserApplication.class)
@ActiveProfiles({"enabled", "ydb"})
class MaxRetriesExhaustedTest extends YdbDockerTest {

    @Autowired
    private UserService userService;

    @BeforeEach
    void cleanUp() {
        DeterministicErrorChannel.configure();
        DeterministicErrorChannel.resetCounters();
        userService.deleteAll();
    }

    @Test
    void shouldExhaustMaxRetriesAndThrow() {
        DeterministicErrorChannel.configure()
                .onError("executeQuery", 1, StatusCode.ABORTED)
                .onError("executeQuery", 2, StatusCode.ABORTED)
                .onError("executeQuery", 3, StatusCode.ABORTED)
                .onError("executeQuery", 4, StatusCode.ABORTED);

        assertThrows(
                Exception.class,
                () -> userService.saveWithMaxAttempts4(createUser(1L, "user1", "first1", "last1")));
        assertEquals(4, DeterministicErrorChannel.getCallCount("executeQuery"));
    }

    @Test
    void shouldSucceedOnLastAttemptMaxRetries() {
        DeterministicErrorChannel.configure()
                .onError("executeQuery", 1, StatusCode.ABORTED)
                .onError("executeQuery", 2, StatusCode.ABORTED);

        userService.saveWithMaxAttempts4(createUser(2L, "user2", "first2", "last2"));

        assertEquals(3, DeterministicErrorChannel.getCallCount("executeQuery"));
        assertNotNull(userService.findById(2L));
    }

    private User createUser(Long id, String username, String firstname, String lastname) {
        return new User(id, username, firstname, lastname);
    }
}
