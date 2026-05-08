package tech.ydb.retry;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.ydb.core.StatusCode.ABORTED;
import static tech.ydb.core.StatusCode.BAD_SESSION;
import static tech.ydb.core.StatusCode.CLIENT_CANCELLED;
import static tech.ydb.core.StatusCode.CLIENT_RESOURCE_EXHAUSTED;
import static tech.ydb.core.StatusCode.OVERLOADED;
import static tech.ydb.core.StatusCode.SESSION_BUSY;
import static tech.ydb.core.StatusCode.SESSION_EXPIRED;
import static tech.ydb.core.StatusCode.TIMEOUT;
import static tech.ydb.core.StatusCode.TRANSPORT_UNAVAILABLE;
import static tech.ydb.core.StatusCode.UNDETERMINED;

class YdbTransactionalConfigOverrideTest extends InterceptorTestSupport {

    @Test
    void shouldOverrideMaxRetriesFromAnnotation() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 1, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(ABORTED), "ok");

        Object result = interceptor.invoke(invocationFor("ydbCustomRetry"));

        assertEquals("ok", result);
        assertEquals(1, interceptor.retries());
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldUseConfigMaxRetriesWhenAnnotationNotSet() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 2, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(BAD_SESSION), "ok");

        Object result = interceptor.invoke(invocationFor("defaultRetry"));

        assertEquals("ok", result);
        assertEquals(1, interceptor.retries());
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldExhaustAnnotatedMaxRetriesAndPropagate() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 1, 0, 0, 0, 0);
        interceptor.enqueueOutcome(
                new ConfigurableStatusException(SESSION_BUSY),
                new ConfigurableStatusException(OVERLOADED),
                new ConfigurableStatusException(OVERLOADED));

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("ydbCustomRetry")));

        assertEquals(OVERLOADED, exception.getStatus().getCode());
        assertEquals(2, interceptor.retries());
        assertEquals(3, interceptor.allInvocations());
    }

    @Test
    void shouldUseAnnotatedMaxRetriesWhenLowerThanConfig() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 1, 0, 0, 0, 0);
        interceptor.enqueueOutcome(
                new ConfigurableStatusException(OVERLOADED),
                new ConfigurableStatusException(BAD_SESSION),
                new ConfigurableStatusException(OVERLOADED));

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("ydbCustomRetry")));

        assertEquals(OVERLOADED, exception.getStatus().getCode());
        assertEquals(2, interceptor.retries());
        assertEquals(3, interceptor.allInvocations());
    }

    @Test
    void shouldUseAnnotatedMaxRetriesWhenHigherThanConfig() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 1, 0, 0, 0, 0);
        interceptor.enqueueOutcome(
                new ConfigurableStatusException(BAD_SESSION),
                new ConfigurableStatusException(SESSION_BUSY),
                new ConfigurableStatusException(ABORTED),
                new ConfigurableStatusException(OVERLOADED),
                "ok");

        Object result = interceptor.invoke(invocationFor("ydbRequiredRetry"));

        assertEquals("ok", result);
        assertEquals(5, interceptor.allInvocations());
    }

    @Test
    void shouldRetryDifferentStatusCodesAcrossRetries() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 1, 0, 0, 0, 0);
        interceptor.enqueueOutcome(
                new ConfigurableStatusException(ABORTED),
                new ConfigurableStatusException(BAD_SESSION),
                "ok");

        Object result = interceptor.invoke(invocationFor("ydbRequiredRetry"));

        assertEquals("ok", result);
        assertEquals(3, interceptor.allInvocations());
    }

    @Test
    void shouldNotRetryClientCancelledWhenNotIdempotent() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(CLIENT_CANCELLED), "ok");

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("ydbNonIdempotentRetry")));

        assertEquals(CLIENT_CANCELLED, exception.getStatus().getCode());
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldRetryClientCancelledWhenIdempotent() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(CLIENT_CANCELLED), "ok");

        Object result = interceptor.invoke(invocationFor("ydbIdempotentRetry"));

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldNotRetryTransportUnavailableWhenNotIdempotent() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(TRANSPORT_UNAVAILABLE), "ok");

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("ydbNonIdempotentRetry")));

        assertEquals(TRANSPORT_UNAVAILABLE, exception.getStatus().getCode());
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldRetryTransportUnavailableWhenIdempotent() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(TRANSPORT_UNAVAILABLE), "ok");

        Object result = interceptor.invoke(invocationFor("ydbIdempotentRetry"));

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldRetryClientResourceExhaustedWhenNotIdempotent() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(CLIENT_RESOURCE_EXHAUSTED), "ok");

        Object result = interceptor.invoke(invocationFor("ydbNonIdempotentRetry"));

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldRetryClientResourceExhaustedWhenIdempotent() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(CLIENT_RESOURCE_EXHAUSTED), "ok");

        Object result = interceptor.invoke(invocationFor("ydbIdempotentRetry"));

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldNotRetryTimeoutWhenIdempotent() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(TIMEOUT));

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("ydbIdempotentRetry")));

        assertEquals(TIMEOUT, exception.getStatus().getCode());
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldNotRetrySessionExpiredWhenNotIdempotent() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(SESSION_EXPIRED));

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("ydbNonIdempotentRetry")));

        assertEquals(SESSION_EXPIRED, exception.getStatus().getCode());
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldRetryAlwaysRetryableCodesWhenIdempotent() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(ABORTED), "ok");

        Object result = interceptor.invoke(invocationFor("ydbIdempotentRetry"));

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldRetryMixedStatusCodesWhenIdempotent() throws Throwable {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(
                new ConfigurableStatusException(ABORTED),
                new ConfigurableStatusException(UNDETERMINED),
                new ConfigurableStatusException(CLIENT_CANCELLED),
                "ok");

        Object result = interceptor.invoke(invocationFor("ydbIdempotentRetry"));

        assertEquals("ok", result);
        assertEquals(4, interceptor.allInvocations());
    }

    @Test
    void shouldNotRetrySessionExpiredWhenIdempotent() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(SESSION_EXPIRED));

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("ydbIdempotentRetry")));

        assertEquals(SESSION_EXPIRED, exception.getStatus().getCode());
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldStopAtIdempotentOnlyCodeWhenNotIdempotent() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 5, 0, 0, 0, 0);
        interceptor.enqueueOutcome(
                new ConfigurableStatusException(BAD_SESSION), new ConfigurableStatusException(TIMEOUT));

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("ydbNonIdempotentRetry")));

        assertEquals(TIMEOUT, exception.getStatus().getCode());
        assertEquals(2, interceptor.allInvocations());
    }

    @Test
    void shouldNotReachDelayCalculatorForTimeoutWhenIdempotent() {
        List<Long> delays = new ArrayList<>();
        TestableInterceptor interceptor =
                interceptorWithSleeper(true, 5, 100, 50, 1000, 500, delays::add);
        interceptor.enqueueOutcome(new ConfigurableStatusException(TIMEOUT));

        assertThrows(
                ConfigurableStatusException.class,
                () -> interceptor.invoke(invocationFor("ydbIdempotentRetry")));

        assertEquals(1, interceptor.allInvocations());
        assertEquals(0, delays.size());
    }

    @Test
    void shouldNotReachDelayCalculatorForSessionExpiredWhenIdempotent() {
        List<Long> delays = new ArrayList<>();
        TestableInterceptor interceptor =
                interceptorWithSleeper(true, 5, 100, 50, 1000, 500, delays::add);
        interceptor.enqueueOutcome(new ConfigurableStatusException(SESSION_EXPIRED));

        assertThrows(
                ConfigurableStatusException.class,
                () -> interceptor.invoke(invocationFor("ydbIdempotentRetry")));

        assertEquals(1, interceptor.allInvocations());
        assertEquals(0, delays.size());
    }

    @Test
    void shouldUseFastBackoffForUndeterminedWhenIdempotent() throws Throwable {
        List<Long> delays = new ArrayList<>();
        TestableInterceptor interceptor =
                interceptorWithSleeper(true, 5, 100, 50, 1000, 500, delays::add);
        interceptor.enqueueOutcome(new ConfigurableStatusException(UNDETERMINED), "ok");

        interceptor.invoke(invocationFor("ydbIdempotentRetry"));

        assertEquals(1, delays.size());
        assertTrue(delays.getFirst() >= 0);
    }

    @Test
    void shouldDelayFirstOverloadedRetryUsingZeroBasedRetryIndex() throws Throwable {
        List<Long> delays = new ArrayList<>();
        TestableInterceptor interceptor = interceptorWithSleeper(true, 5, 1, 1, 1, 1, delays::add);
        interceptor.enqueueOutcome(new ConfigurableStatusException(OVERLOADED), "ok");

        Object result = interceptor.invoke(invocationFor("ydbCustomRetry"));

        assertEquals("ok", result);
        assertEquals(2, interceptor.allInvocations());
        assertEquals(List.of(1L), delays);
    }

    @Test
    void shouldNotRetryWhenMethodDisablesRetry() {
        TestableInterceptor interceptor = interceptorWithConfig(true, 3, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(BAD_SESSION), "ok");

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("ydbRetryDisabled")));

        assertEquals(BAD_SESSION, exception.getStatus().getCode());
        assertEquals(1, interceptor.allInvocations());
    }

    @Test
    void shouldNotRetryWhenGlobalConfigDisablesRetryEvenIfMethodEnablesIt() {
        TestableInterceptor interceptor = interceptorWithConfig(false, 3, 0, 0, 0, 0);
        interceptor.enqueueOutcome(new ConfigurableStatusException(BAD_SESSION), "ok");

        ConfigurableStatusException exception =
                assertThrows(
                        ConfigurableStatusException.class,
                        () -> interceptor.invoke(invocationFor("ydbRetryEnabled")));

        assertEquals(BAD_SESSION, exception.getStatus().getCode());
        assertEquals(1, interceptor.allInvocations());
    }
}
