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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = UserApplication.class)
@ActiveProfiles({"enabled", "ydb"})
class CombinedErrorIntegrationTest extends YdbDockerTest {

    @Autowired
    private UserService userService;

    @BeforeEach
    void cleanUp() {
        DeterministicErrorChannel.configure();
        DeterministicErrorChannel.resetCounters();
        userService.deleteAll();
    }

    @Test
    void shouldRetryWhenExecuteQueryFailsThenCommitFails() {
        DeterministicErrorChannel.configure()
                .onError("executeQuery", 1, StatusCode.ABORTED)
                .onError("commitTransaction", 1, StatusCode.BAD_SESSION);

        userService.save(createUser(1L, "user1", "first1", "last1"));

        assertEquals(3, DeterministicErrorChannel.getCallCount("executeQuery"));
        assertEquals(2, DeterministicErrorChannel.getCallCount("commitTransaction"));
        assertNotNull(userService.findById(1L));
    }

    @Test
    void shouldRetryWhenExecuteQueryFailsTwiceThenCommitFails() {
        DeterministicErrorChannel.configure()
                .onError("executeQuery", 1, StatusCode.ABORTED)
                .onError("executeQuery", 2, StatusCode.BAD_SESSION)
                .onError("commitTransaction", 1, StatusCode.SESSION_BUSY);

        userService.save(createUser(2L, "user2", "first2", "last2"));

        assertEquals(4, DeterministicErrorChannel.getCallCount("executeQuery"));
        assertEquals(2, DeterministicErrorChannel.getCallCount("commitTransaction"));
        assertNotNull(userService.findById(2L));
    }

    @Test
    void shouldStopRetryWhenNonRetryableCommitFollowsRetryableExecuteQuery() {
        DeterministicErrorChannel.configure()
                .onError("executeQuery", 1, StatusCode.ABORTED)
                .onError("commitTransaction", 1, StatusCode.SCHEME_ERROR);

        assertThrows(Exception.class, () -> userService.save(createUser(3L, "user3", "first3", "last3")));

        assertEquals(2, DeterministicErrorChannel.getCallCount("executeQuery"));
        assertEquals(1, DeterministicErrorChannel.getCallCount("commitTransaction"));
    }

    @Test
    void shouldRecoverFromMixedExecuteQueryAndCommitErrors() {
        DeterministicErrorChannel.configure()
                .onError("executeQuery", 1, StatusCode.ABORTED)
                .onError("commitTransaction", 1, StatusCode.ABORTED)
                .onError("commitTransaction", 2, StatusCode.BAD_SESSION);

        userService.save(createUser(4L, "user4", "first4", "last4"));

        assertTrue(DeterministicErrorChannel.getCallCount("executeQuery") >= 3);
        assertTrue(DeterministicErrorChannel.getCallCount("commitTransaction") >= 3);
        assertNotNull(userService.findById(4L));
    }

    private User createUser(Long id, String username, String firstname, String lastname) {
        return new User(id, username, firstname, lastname);
    }
}
