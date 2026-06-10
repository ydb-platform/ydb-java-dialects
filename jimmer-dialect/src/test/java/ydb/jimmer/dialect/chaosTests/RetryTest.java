package ydb.jimmer.dialect.chaosTests;

import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ydb.jimmer.dialect.AbstractSelectTest;

public class RetryTest extends AbstractSelectTest {
    @Test
    public void succeedAfter1Failure() {
        int maxAttempts = 2;

        FailFirstN<?> chaosPolicy = new FailFirstN<>(maxAttempts - 1);
        Assertions.assertDoesNotThrow(() ->
                getIsolationClient().transaction(
                        maxAttempts, 0,
                        chaosPolicy
                )
        );

        Assertions.assertEquals(maxAttempts, chaosPolicy.getAttempt());
    }

    @Test
    public void failAllAttempts() {
        int maxAttempts = 3;

        Assertions.assertThrows(
                ExecutionException.class, () ->
                getIsolationClient().transaction(
                        maxAttempts, 0,
                        new AlwaysFail<>()
                )
        );
    }

    @Test
    public void failWithoutRetries() {
        int maxAttempts = 3;

        NotRetryableFail<?> chaosPolicy = new NotRetryableFail<>();
        Assertions.assertThrows(
                ExecutionException.class, () ->
                getIsolationClient().transaction(
                        maxAttempts, 0,
                        chaosPolicy
                )
        );

        Assertions.assertEquals(1, chaosPolicy.getAttempt());
    }

    @Test
    public void exponentialBackoffTest() {
        int maxAttempts = 3;

        FailFirstN<?> chaosPolicy = new FailFirstN<>(maxAttempts - 1);

        long start = System.currentTimeMillis();
        Assertions.assertDoesNotThrow(() ->
                getIsolationClient().transaction(
                        maxAttempts, 100,
                        chaosPolicy
                )
        );
        long elapsed = System.currentTimeMillis() - start;

        Assertions.assertTrue(elapsed >= 300, "the backoff time is less than 300ms");
    }
}
