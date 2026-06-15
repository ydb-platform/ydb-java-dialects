package tech.ydb.retry;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.ydb.core.StatusCode.ABORTED;
import static tech.ydb.core.StatusCode.BAD_SESSION;

class TransactionPropagationRetryTest extends InterceptorTestSupport {

    @Test
    void shouldDisableRetryWhenParticipatingInOuterTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        TestableInterceptor interceptor = interceptorWithConfig(true, 2, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new IllegalStateException("no retry expected"));

        assertThrows(
                IllegalStateException.class, () -> interceptor.invoke(invocationFor("ydbRequiredRetry")));
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldRetryWithRequiresNewInsideOuterTransaction() throws Throwable {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        TestableInterceptor interceptor = interceptorWithConfig(true, 2, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(BAD_SESSION), "ok");

        Object result = interceptor.invoke(invocationFor("ydbRequiresNewRetry"));

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldDisableRetryWithNestedPropagationInsideOuterTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        TestableInterceptor interceptor = interceptorWithConfig(true, 2, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(ABORTED));

        assertThrows(
                ConfigurableStatusException.class,
                () -> interceptor.invoke(invocationFor("ydbNestedRetry")));
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldRetryWithNotSupportedPropagationInsideOuterTransaction() throws Throwable {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        TestableInterceptor interceptor = interceptorWithConfig(true, 2, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(BAD_SESSION), "ok");

        Object result = interceptor.invoke(invocationFor("ydbNotSupportedRetry"));

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
    }
}
