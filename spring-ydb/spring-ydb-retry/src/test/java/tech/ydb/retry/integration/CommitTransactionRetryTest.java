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

    @ParameterizedTest(name = "CommitTransaction")
    @EnumSource(value = StatusCode.class, names = {
            "ABORTED", "UNAVAILABLE", "OVERLOADED", "BAD_SESSION",
            "SESSION_BUSY"
    })
    void shouldRecoverFromRetryableCommitError(StatusCode code) {
        DeterministicErrorChannel.configure().onError("commitTransaction", 1, code);

        userService.save(createUser(1L, "user1", "first1", "last1"));

        assertEquals(2, DeterministicErrorChannel.getCallCount("commitTransaction"));
        assertNotNull(userService.findById(1L));
    }

    @ParameterizedTest(name = "CommitTransaction")
    @EnumSource(value = StatusCode.class, names = {
            "ABORTED", "UNAVAILABLE"
    })
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
