package tech.ydb.retry.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
class CommitTransactionRetryTest extends YdbDockerTest {

    @Autowired
    private UserService userService;

    @BeforeEach
    void cleanUp() {
        DeterministicErrorChannel.configure();
        DeterministicErrorChannel.resetCounters();
        userService.deleteAll();
    }

    /**
     * When the first CommitTransaction RPC returns a retryable status, the retry must (a) re-issue
     * an ExecuteQuery for the INSERT against a fresh transaction and (b) issue a second
     * CommitTransaction RPC. Both gRPC counters are observed via the deterministic error channel.
     * Without (a) the data would never reach the new tx; without (b) Spring would silently swallow
     * a failed commit.
     */
    @ParameterizedTest(name = "CommitTransaction")
    @EnumSource(
            value = StatusCode.class,
            names = {"ABORTED", "UNAVAILABLE", "OVERLOADED", "BAD_SESSION", "SESSION_BUSY"})
    void shouldRecoverFromRetryableCommitError(StatusCode code) {
        DeterministicErrorChannel.configure().onError("commitTransaction", 1, code);

        User user = createUser(1L, "user1", "first1", "last1");
        userService.save(user);

        assertEquals(2, DeterministicErrorChannel.getCallCount("commitTransaction"),
                "commit must be attempted twice: first attempt fails with " + code
                        + ", retry must honestly invoke CommitTransaction again");
        assertTrue(DeterministicErrorChannel.getCallCount("executeQuery") >= 2,
                "INSERT must be re-executed against a fresh transaction on retry");

        User persisted = userService.findById(user.getId());
        assertNotNull(persisted, "user must end up persisted by the successful retry commit");
        assertEquals(user.getUsername(), persisted.getUsername());
    }

    @ParameterizedTest(name = "CommitTransaction")
    @EnumSource(
            value = StatusCode.class,
            names = {"ABORTED", "UNAVAILABLE"})
    void shouldRecoverFromMultipleCommitErrors(StatusCode code) {
        DeterministicErrorChannel.configure()
                .onError("commitTransaction", 1, code)
                .onError("commitTransaction", 2, code);

        userService.save(createUser(2L, "user2", "first2", "last2"));

        assertEquals(3, DeterministicErrorChannel.getCallCount("commitTransaction"));
        assertNotNull(userService.findById(2L));
    }

    private User createUser(Long id, String username, String firstname, String lastname) {
        return new User(id, username, firstname, lastname);
    }
}
