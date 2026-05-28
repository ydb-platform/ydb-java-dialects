package tech.ydb.retry;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.ydb.core.StatusCode.ABORTED;
import static tech.ydb.core.StatusCode.BAD_SESSION;
import static tech.ydb.core.StatusCode.CLIENT_INTERNAL_ERROR;
import static tech.ydb.core.StatusCode.TIMEOUT;
import static tech.ydb.core.StatusCode.UNAUTHORIZED;

class TransactionalDefaultRetryTest extends InterceptorTestSupport {

    @Test
    void shouldRetryWithDefaultConfigUntilSuccess() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 3, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(BAD_SESSION), "ok");

        Object result = interceptor.invoke(invocationFor("regularTx"));

        assertEquals("ok", result);
        assertEquals(1, interceptor.retries());
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldExhaustDefaultMaxRetriesAndPropagateLastException() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 2, 0, 0, 0, 0);
        interceptor.enqueueOutcome(
                new ConfigurableStatusException(BAD_SESSION),
                new ConfigurableStatusException(ABORTED),
                new ConfigurableStatusException(ABORTED));

        assertThrows(
                ConfigurableStatusException.class, () -> interceptor.invoke(invocationFor("regularTx")));

        assertEquals(2, interceptor.retries());
        assertEquals(3, interceptor.allInvocations());
    }

    @Test
    void shouldPropagateNonRetryableExceptionImmediately() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(UNAUTHORIZED));

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("regularTx")));

        assertEquals(UNAUTHORIZED, exception.statusCode());
        assertEquals(0, interceptor.retries());
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldNotRetryNonYdbRuntimeException() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new IllegalStateException("not ydb"));

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class, () -> interceptor.invoke(invocationFor("regularTx")));

        assertEquals("not ydb", exception.getMessage());
        assertEquals(0, interceptor.retries());
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldImmediatelyPropagateJavaError() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new OutOfMemoryError("test oom"));

        assertThrows(OutOfMemoryError.class, () -> interceptor.invoke(invocationFor("regularTx")));
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldRetryWhenYdbStatusExtractedFromExceptionChain() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 3, 0, 0, 0, 0);
        interceptor.enqueueOutcome(
                new RuntimeException("wrapped", new ConfigurableStatusException(BAD_SESSION)), "ok");

        Object result = interceptor.invoke(invocationFor("regularTx"));

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldCallSleeperWithBackoffDelay() throws Throwable {
        List<Long> delays = new ArrayList<>();
        TestableInterceptor interceptor = interceptorWithSleeper(true, 5, 0, 0, 0, 0, delays::add);
        interceptor.enqueueOutcome(
                new ConfigurableStatusException(ABORTED), new ConfigurableStatusException(ABORTED), "ok");

        Object result = interceptor.invoke(invocationFor("regularTx"));

        assertEquals("ok", result);
        assertEquals(3, interceptor.allInvocations());
        assertEquals(2, delays.size());
        for (Long delay : delays) {
            assertTrue(delay >= 0);
        }
    }

    @Test
    void shouldUseZeroDelayForBadSession() throws Throwable {
        List<Long> delays = new ArrayList<>();
        TestableInterceptor interceptor =
                interceptorWithSleeper(true, 5, 100, 50, 1000, 500, delays::add);
        interceptor.enqueueOutcome(new ConfigurableStatusException(BAD_SESSION), "ok");

        Object result = interceptor.invoke(invocationFor("regularTx"));

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
        assertEquals(1, delays.size());
        assertEquals(0, delays.get(0));
    }

    @Test
    void shouldHandleInterruptedSleep() {
        ConfigurableStatusException originalException = new ConfigurableStatusException(ABORTED);
        TestableInterceptor interceptor =
                interceptorWithSleeper(
                        true,
                        3,
                        0,
                        0,
                        0,
                        0,
                        delay -> {
                            throw new InterruptedException("sleep interrupted");
                        });
        interceptor.enqueueOutcome(originalException, "ok");

        try {
            InterruptedException thrown =
                    assertThrows(
                            InterruptedException.class,
                            () -> interceptor.invoke(invocationFor("ydbIdempotentRetry")));
            assertEquals("sleep interrupted", thrown.getMessage());
            assertEquals(1, thrown.getSuppressed().length);
            assertSame(originalException, thrown.getSuppressed()[0]);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void shouldNotRetryClientInternalErrorForTransactionalMethod() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 3, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(CLIENT_INTERNAL_ERROR), "ok");

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("regularTx")));

        assertEquals(CLIENT_INTERNAL_ERROR, exception.statusCode());
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldNotRetryTimeoutForTransactionalMethodWhenDefaultConfigNotIdempotent() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 3, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(TIMEOUT));

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("regularTx")));

        assertEquals(TIMEOUT, exception.statusCode());
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldNotRetryWhenDisabledInConfig() {
        TestableInterceptor interceptor = interceptorWithConfig(false, 3, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(BAD_SESSION), "ok");

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("regularTx")));

        assertEquals(BAD_SESSION, exception.statusCode());
        assertEquals(1, interceptor.allInvocations());
    }
}
