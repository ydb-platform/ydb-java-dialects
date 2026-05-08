package tech.ydb.retry.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = UserApplication.class)
@ActiveProfiles({"enabled", "ydb"})
class ExecuteQueryRetryIntegrationTest extends YdbDockerTest {

    @Autowired
    private UserService userService;

    @BeforeEach
    void cleanUp() {
        DeterministicErrorChannel.configure();
        DeterministicErrorChannel.resetCounters();
        userService.deleteAll();
    }

    @ParameterizedTest(name = "ExecuteQuery")
    @EnumSource(
            value = StatusCode.class,
            names = {"ABORTED", "UNAVAILABLE", "OVERLOADED", "BAD_SESSION", "SESSION_BUSY"})
    void shouldRecoverFromRetryableError(StatusCode code) {
        DeterministicErrorChannel.configure().onError("executeQuery", 1, code);

        userService.save(createUser(1L, "user1", "first1", "last1"));

        assertEquals(2, DeterministicErrorChannel.getCallCount("executeQuery"));
        assertNotNull(userService.findById(1L));
    }

    @ParameterizedTest(name = "ExecuteQuery")
    @EnumSource(
            value = StatusCode.class,
            names = {"ABORTED", "UNAVAILABLE", "OVERLOADED", "BAD_SESSION"})
    void shouldRecoverFromMultipleRetryableErrors(StatusCode code) {
        DeterministicErrorChannel.configure()
                .onError("executeQuery", 1, code)
                .onError("executeQuery", 2, code);

        userService.save(createUser(2L, "user2", "first2", "last2"));

        assertEquals(3, DeterministicErrorChannel.getCallCount("executeQuery"));
        assertNotNull(userService.findById(2L));
    }

    @Test
    void shouldRecoverFromMixedErrors() {
        DeterministicErrorChannel.configure()
                .onError("executeQuery", 1, StatusCode.ABORTED)
                .onError("executeQuery", 2, StatusCode.BAD_SESSION);

        userService.save(createUser(3L, "user3", "first3", "last3"));

        assertEquals(3, DeterministicErrorChannel.getCallCount("executeQuery"));
        assertNotNull(userService.findById(3L));
    }

    @ParameterizedTest(name = "ExecuteQuery")
    @EnumSource(
            value = StatusCode.class,
            names = {"SCHEME_ERROR", "GENERIC_ERROR", "PRECONDITION_FAILED"})
    void shouldNotRetryNonRetryableError(StatusCode code) {
        DeterministicErrorChannel.configure().onError("executeQuery", 1, code);

        assertThrows(
                Exception.class, () -> userService.save(createUser(4L, "user4", "first4", "last4")));
        assertEquals(1, DeterministicErrorChannel.getCallCount("executeQuery"));
    }

    private User createUser(Long id, String username, String firstname, String lastname) {
        return new User(id, username, firstname, lastname);
    }
}
