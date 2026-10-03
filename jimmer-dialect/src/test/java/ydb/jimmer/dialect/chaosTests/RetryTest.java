package ydb.jimmer.dialect.chaosTests;

import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ydb.jimmer.dialect.AbstractSelectTest;
import ydb.jimmer.dialect.transaction.RetryConfig;

public class RetryTest extends AbstractSelectTest {
    @Test
    public void succeedAfter1Failure() {
        int maxAttempts = 2;

        FailFirstN<?> chaosPolicy = new FailFirstN<>(maxAttempts - 1);
        Assertions.assertDoesNotThrow(() ->
                getIsolationClient().transaction(
                        RetryConfig.builder()
                                .maxAttempts(maxAttempts)
                                .backoffBaseMs(0)
                                .build(),
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
                        RetryConfig.builder()
                                .maxAttempts(maxAttempts)
                                .backoffBaseMs(0)
                                .build(),
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
                        RetryConfig.builder()
                                .maxAttempts(maxAttempts)
                                .backoffBaseMs(0)
                                .build(),
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
                        RetryConfig.builder()
                                .maxAttempts(maxAttempts)
                                .backoffBaseMs(100)
                                .build(),
                        chaosPolicy
                )
        );
        long elapsed = System.currentTimeMillis() - start;

        Assertions.assertTrue(elapsed >= 150, "the backoff time is less than 150ms");
    }
}
