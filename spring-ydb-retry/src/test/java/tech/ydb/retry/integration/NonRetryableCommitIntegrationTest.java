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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = UserApplication.class)
@ActiveProfiles({"enabled", "ydb"})
class NonRetryableCommitIntegrationTest extends YdbDockerTest {

    @Autowired
    private UserService userService;

    @BeforeEach
    void cleanUp() {
        DeterministicErrorChannel.configure();
        DeterministicErrorChannel.resetCounters();
        userService.deleteAll();
    }

    @ParameterizedTest(name = "NonRetryableCommit")
    @EnumSource(
            value = StatusCode.class,
            names = {"SCHEME_ERROR", "GENERIC_ERROR", "PRECONDITION_FAILED", "UNAUTHORIZED"})
    void shouldNotRetryNonRetryableCommitError(StatusCode code) {
        DeterministicErrorChannel.configure().onError("commitTransaction", 1, code);

        assertThrows(
                Exception.class, () -> userService.save(createUser(1L, "user1", "first1", "last1")));
        assertEquals(1, DeterministicErrorChannel.getCallCount("commitTransaction"));
        assertNull(userService.findById(1L));
    }

    @ParameterizedTest(name = "NonRetryableCommit")
    @EnumSource(
            value = StatusCode.class,
            names = {"SCHEME_ERROR", "GENERIC_ERROR", "PRECONDITION_FAILED", "UNAUTHORIZED"})
    void shouldNotRetryNonRetryableCommitErrorWithYdbTransactional(StatusCode code) {
        DeterministicErrorChannel.configure().onError("commitTransaction", 1, code);

        assertThrows(
                Exception.class,
                () -> userService.saveWithMaxAttempts4(createUser(2L, "user2", "first2", "last2")));
        assertEquals(1, DeterministicErrorChannel.getCallCount("commitTransaction"));
        assertNull(userService.findById(2L));
    }

    private User createUser(Long id, String username, String firstname, String lastname) {
        return new User(id, username, firstname, lastname);
    }
}
