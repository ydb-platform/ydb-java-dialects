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

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = UserApplication.class)
@ActiveProfiles({"disabled", "ydb"})
class DisabledRetryIntegrationTest extends YdbDockerTest {

    @Autowired
    private UserService userService;

    @BeforeEach
    void cleanUp() {
        DeterministicErrorChannel.configure();
        userService.deleteAll();
    }

    @ParameterizedTest(name = "Retry disabled")
    @EnumSource(
            value = StatusCode.class,
            names = {"ABORTED", "UNAVAILABLE", "OVERLOADED"})
    void shouldNotRetryWhenRetryDisabledExecuteQuery(StatusCode code) {
        DeterministicErrorChannel.configure().onError("executeQuery", 1, code);

        assertThrows(
                Exception.class, () -> userService.saveRaw(createUser(1L, "user1", "first1", "last1")));
    }

    @ParameterizedTest(name = "Retry disabled")
    @EnumSource(
            value = StatusCode.class,
            names = {"ABORTED", "UNAVAILABLE", "OVERLOADED"})
    void shouldNotRetryWhenRetryDisabledCommit(StatusCode code) {
        DeterministicErrorChannel.configure().onError("commitTransaction", 1, code);

        assertThrows(
                Exception.class, () -> userService.saveRaw(createUser(2L, "user2", "first2", "last2")));
    }

    private User createUser(Long id, String username, String firstname, String lastname) {
        return new User(id, username, firstname, lastname);
    }
}
