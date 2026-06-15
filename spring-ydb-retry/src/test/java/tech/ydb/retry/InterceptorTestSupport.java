package tech.ydb.retry;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tech.ydb.core.StatusCode;

abstract class InterceptorTestSupport {

    @AfterEach
    void cleanupTransactionContext() {
        TransactionSynchronizationManager.clear();
    }

    static TestableInterceptor interceptorWithConfig(
            boolean enabled, int maxAttempts, int slowBase, int fastBase, int slowCap, int fastCap) {
        return interceptorWithSleeper(
                enabled, maxAttempts, slowBase, fastBase, slowCap, fastCap, delay -> {
                });
    }

    static TestableInterceptor interceptorWithSleeper(
            boolean enabled,
            int maxAttempts,
            int slowBase,
            int fastBase,
            int slowCap,
            int fastCap,
            BackoffSleeper sleeper) {
        TestableInterceptor interceptor =
                new TestableInterceptor(
                        new YdbRetryPolicyConfig(enabled, maxAttempts, slowBase, fastBase, slowCap, fastCap),
                        sleeper);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        return interceptor;
    }

    static MethodInvocation invocationFor(String methodName) {
        Method method = methodOf(methodName);
        Object target = targetFor(methodName);
        return invocationFor(method, target);
    }

    static MethodInvocation invocationFor(Method method, Object target) {
        MethodInvocation invocation = Mockito.mock(MethodInvocation.class);
        Mockito.when(invocation.getMethod()).thenReturn(method);
        Mockito.when(invocation.getThis()).thenReturn(target);
        Mockito.when(invocation.getArguments()).thenReturn(new Object[0]);
        return invocation;
    }

    private static Object targetFor(String methodName) {
        if (methodName.startsWith("ydb") || methodName.startsWith("default")) {
            return new YdbTransactionalTestService();
        }
        return new TransactionalTestService();
    }

    static Method methodOf(String methodName) {
        try {
            if (methodName.startsWith("ydb") || methodName.startsWith("default")) {
                return YdbTransactionalTestService.class.getMethod(methodName);
            }
            return TransactionalTestService.class.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    static final class TestableInterceptor extends YdbTransactionInterceptor {
        private final Deque<Object> outcomes = new ArrayDeque<>();
        private final AtomicInteger attempts = new AtomicInteger();

        TestableInterceptor(YdbRetryPolicyConfig retryConfig, BackoffSleeper backoffSleeper) {
            super(retryConfig, backoffSleeper);
        }

        void enqueueOutcome(Object... results) {
            for (Object result : results) {
                outcomes.addLast(result);
            }
        }

        int allInvocations() {
            return attempts.get();
        }

        int retries() {
            return Math.max(0, attempts.get() - 1);
        }

        @Override
        protected Object invokeWithinTransaction(
                Method method, Class<?> targetClass, InvocationCallback invocation) throws Throwable {
            try {
                invocation.proceedWithInvocation();
            } catch (Throwable ignored) {

            }
            attempts.incrementAndGet();
            Object result = outcomes.removeFirst();
            if (result instanceof Throwable throwable) {
                throw throwable;
            }
            return result;
        }
    }

    static class TransactionalTestService {
        @Transactional
        public String regularTx() {
            return "ok";
        }
    }

    static class YdbTransactionalTestService {
        @YdbTransactional(maxAttempts = 3)
        public String ydbCustomRetry() {
            return "ok";
        }

        @YdbTransactional(maxAttempts = 6)
        public String ydbRequiredRetry() {
            return "ok";
        }

        @YdbTransactional(maxAttempts = 3, propagation = Propagation.REQUIRES_NEW)
        public String ydbRequiresNewRetry() {
            return "ok";
        }

        @YdbTransactional(maxAttempts = 4, propagation = Propagation.NESTED)
        public String ydbNestedRetry() {
            return "ok";
        }

        @YdbTransactional(maxAttempts = 4, propagation = Propagation.NOT_SUPPORTED)
        public String ydbNotSupportedRetry() {
            return "ok";
        }

        @YdbTransactional
        public String defaultRetry() {
            return "ok";
        }

        @YdbTransactional(enabled = false)
        public String ydbRetryDisabled() {
            return "ok";
        }

        @YdbTransactional(enabled = true)
        public String ydbRetryEnabled() {
            return "ok";
        }

        @YdbTransactional("customTransactionManager")
        public String ydbValueAliasManager() {
            return "ok";
        }

        @YdbTransactional(timeoutString = "15")
        public String ydbTimeoutString() {
            return "ok";
        }

        @YdbTransactional(
                maxAttempts = 100,
                slowBackoffBaseMs = 200,
                fastBackoffBaseMs = 10,
                slowCapBackoffMs = 10000,
                fastCapBackoffMs = 12)
        public String ydbNewTransactionSettings() {
            return "ok";
        }

        @YdbTransactional(maxAttempts = -2)
        public String ydbNegativeMaxAttempts() {
            return "ok";
        }

        @YdbTransactional(maxAttempts = 0)
        public String ydbZeroMaxAttempts() {
            return "ok";
        }

        @YdbTransactional(maxAttempts = 6, idempotent = true)
        public String ydbIdempotentRetry() {
            return "ok";
        }

        @YdbTransactional(maxAttempts = 4)
        public String ydbNonIdempotentRetry() {
            return "ok";
        }
    }

    /**
     * Mimics what {@code tech.ydb.jdbc.exception.ExceptionFactory} produces at runtime: a
     * {@link RuntimeException} that wraps a {@link SQLException} whose {@code errorCode} is the
     * {@link StatusCode#getCode() YDB status code}. This is exactly the shape that the JDBC driver
     * propagates and what Spring-Data wraps into a {@code DataAccessException}.
     */
    static final class ConfigurableStatusException extends RuntimeException {
        private final StatusCode statusCode;

        ConfigurableStatusException(StatusCode statusCode) {
            super("test exception with status " + statusCode, new SQLException(
                    "test exception with status " + statusCode, null, statusCode.getCode()));
            this.statusCode = statusCode;
        }

        StatusCode statusCode() {
            return statusCode;
        }
    }
}
