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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = UserApplication.class)
@ActiveProfiles({"enabled", "ydb"})
class IdempotentRetryIntegrationTest extends YdbDockerTest {

    @Autowired
    private UserService userService;

    @BeforeEach
    void cleanUp() {
        DeterministicErrorChannel.configure();
        DeterministicErrorChannel.resetCounters();
        userService.deleteAll();
    }

    @ParameterizedTest(name = "Idempotent executeQuery non-retryable")
    @EnumSource(
            value = StatusCode.class,
            names = {"TIMEOUT", "SESSION_EXPIRED"})
    void shouldNotRetryTimeoutOrSessionExpiredWhenIdempotentExecuteQuery(StatusCode code) {
        DeterministicErrorChannel.configure().onError("executeQuery", 1, code);

        assertThrows(
                Exception.class,
                () -> userService.saveIdempotent(createUser(1L, "user1", "first1", "last1")));

        assertEquals(1, DeterministicErrorChannel.getCallCount("executeQuery"));
        assertNull(userService.findById(1L));
    }

    @ParameterizedTest(name = "Non-idempotent executeQuery")
    @EnumSource(
            value = StatusCode.class,
            names = {"TIMEOUT", "SESSION_EXPIRED", "UNDETERMINED"})
    void shouldNotRetryUndeterminedOrNonRetryableStatusWhenNotIdempotentExecuteQuery(
            StatusCode code) {
        DeterministicErrorChannel.configure().onError("executeQuery", 1, code);

        assertThrows(
                Exception.class, () -> userService.save(createUser(2L, "user2", "first2", "last2")));
        assertEquals(1, DeterministicErrorChannel.getCallCount("executeQuery"));
        assertNull(userService.findById(2L));
    }

    @ParameterizedTest(name = "Idempotent executeQuery")
    @EnumSource(
            value = StatusCode.class,
            names = {"UNDETERMINED"})
    void shouldRetryUndeterminedWhenIdempotentExecuteQuery(StatusCode code) {
        DeterministicErrorChannel.configure().onError("executeQuery", 1, code);

        userService.saveIdempotent(createUser(3L, "user3", "first3", "last3"));

        assertEquals(2, DeterministicErrorChannel.getCallCount("executeQuery"));
        assertNotNull(userService.findById(3L));
    }

    @ParameterizedTest(name = "Idempotent commit non-retryable")
    @EnumSource(
            value = StatusCode.class,
            names = {"TIMEOUT", "SESSION_EXPIRED"})
    void shouldNotRetryTimeoutOrSessionExpiredWhenIdempotentCommit(StatusCode code) {
        DeterministicErrorChannel.configure().onError("commitTransaction", 1, code);

        assertThrows(
                Exception.class,
                () -> userService.saveIdempotent(createUser(4L, "user4", "first4", "last4")));

        assertEquals(1, DeterministicErrorChannel.getCallCount("commitTransaction"));
    }

    @ParameterizedTest(name = "Idempotent commit")
    @EnumSource(
            value = StatusCode.class,
            names = {"UNDETERMINED"})
    void shouldRetryUndeterminedWhenIdempotentCommit(StatusCode code) {
        DeterministicErrorChannel.configure().onError("commitTransaction", 1, code);

        userService.saveIdempotent(createUser(5L, "user5", "first5", "last5"));

        assertEquals(2, DeterministicErrorChannel.getCallCount("commitTransaction"));
        assertNotNull(userService.findById(5L));
    }

    @ParameterizedTest(name = "Non-idempotent commit")
    @EnumSource(
            value = StatusCode.class,
            names = {"UNDETERMINED"})
    void shouldNotRetryUndeterminedWhenNotIdempotentCommit(StatusCode code) {
        DeterministicErrorChannel.configure().onError("commitTransaction", 1, code);

        assertThrows(
                Exception.class, () -> userService.save(createUser(6L, "user6", "first6", "last6")));

        assertEquals(1, DeterministicErrorChannel.getCallCount("commitTransaction"));
    }

    private User createUser(Long id, String username, String firstname, String lastname) {
        return new User(id, username, firstname, lastname);
    }
}
