package tech.ydb.retry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.ydb.core.StatusCode.ABORTED;
import static tech.ydb.core.StatusCode.BAD_SESSION;

/**
 * Locks in the core "retrier captures the whole transaction" contract: the YDB retry interceptor
 * must start a brand-new Spring transaction on every retry attempt and roll back the previous one.
 *
 * <p>Unlike the rest of the suite, this test drives the real {@link YdbTransactionInterceptor}
 * against a counting {@link PlatformTransactionManager} so that we observe Spring's actual
 * begin/commit/rollback cycle, not a stubbed-out {@code invokeWithinTransaction}.
 */
class RetryStartsFreshTransactionTest extends InterceptorTestSupport {

    @Test
    void shouldStartFreshTransactionForEveryRetryAttempt() throws Throwable {
        CountingTransactionManager txManager = new CountingTransactionManager();
        YdbTransactionInterceptor interceptor = newInterceptor(txManager, 3);

        ProxyMethodInvocation invocation = invocationReturning(
                "ydbCustomRetry",
                new ConfigurableStatusException(ABORTED),
                new ConfigurableStatusException(BAD_SESSION),
                "ok");

        Object result = interceptor.invoke(invocation);

        assertEquals("ok", result);
        assertEquals(3, txManager.beginCount(), "every retry must begin a new transaction");
        assertEquals(2, txManager.rollbackCount(), "each failing attempt must roll back its own tx");
        assertEquals(1, txManager.commitCount(), "only the final successful attempt must commit");
    }

    @Test
    void shouldRollbackEveryAttemptAndPropagateWhenMaxRetriesExhausted() {
        CountingTransactionManager txManager = new CountingTransactionManager();
        YdbTransactionInterceptor interceptor = newInterceptor(txManager, 2);

        ProxyMethodInvocation invocation = invocationReturning(
                "ydbCustomRetry",
                new ConfigurableStatusException(ABORTED),
                new ConfigurableStatusException(ABORTED),
                new ConfigurableStatusException(ABORTED));

        assertThrows(ConfigurableStatusException.class, () -> interceptor.invoke(invocation));

        assertEquals(3, txManager.beginCount(), "every attempt up to maxAttempts must begin a tx");
        assertEquals(3, txManager.rollbackCount(), "all three attempts must roll back");
        assertEquals(0, txManager.commitCount(), "nothing must be committed");
    }

    @Test
    void shouldNotRetryNonYdbExceptionAndRollbackOnlyOnce() {
        CountingTransactionManager txManager = new CountingTransactionManager();
        YdbTransactionInterceptor interceptor = newInterceptor(txManager, 5);

        ProxyMethodInvocation invocation =
                invocationReturning("ydbCustomRetry", new IllegalStateException("non-ydb"));

        assertThrows(IllegalStateException.class, () -> interceptor.invoke(invocation));

        assertEquals(1, txManager.beginCount());
        assertEquals(1, txManager.rollbackCount());
        assertEquals(0, txManager.commitCount());
    }

    @Test
    void shouldBeginAndCommitOnceForHappyPath() throws Throwable {
        CountingTransactionManager txManager = new CountingTransactionManager();
        YdbTransactionInterceptor interceptor = newInterceptor(txManager, 3);

        ProxyMethodInvocation invocation = invocationReturning("ydbCustomRetry", "ok");

        Object result = interceptor.invoke(invocation);

        assertEquals("ok", result);
        assertEquals(1, txManager.beginCount());
        assertEquals(0, txManager.rollbackCount());
        assertEquals(1, txManager.commitCount());
    }

    private static YdbTransactionInterceptor newInterceptor(
            PlatformTransactionManager txManager, int maxAttempts) {
        YdbTransactionInterceptor interceptor = new YdbTransactionInterceptor(
                new YdbRetryPolicyConfig(true, maxAttempts, 0, 0, 0, 0), delay -> {
                });
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        interceptor.setTransactionManager(txManager);
        return interceptor;
    }

    /**
     * Builds a {@link ProxyMethodInvocation} whose {@code invocableClone()} hands out a fresh stub
     * for every attempt, mimicking Spring's reflective method-invocation contract under retry.
     */
    private static ProxyMethodInvocation invocationReturning(String methodName, Object... outcomes) {
        Method method = methodOf(methodName);
        Object target = new YdbTransactionalTestService();

        List<ProxyMethodInvocation> clones = new ArrayList<>(outcomes.length);
        for (Object outcome : outcomes) {
            ProxyMethodInvocation clone = stubInvocation(method, target);
            stubProceed(clone, outcome);
            clones.add(clone);
        }

        ProxyMethodInvocation root = stubInvocation(method, target);
        if (clones.size() == 1) {
            Mockito.when(root.invocableClone()).thenReturn(clones.get(0));
        } else {
            ProxyMethodInvocation[] tail = clones.subList(1, clones.size())
                    .toArray(new ProxyMethodInvocation[0]);
            Mockito.when(root.invocableClone()).thenReturn(clones.get(0), tail);
        }
        return root;
    }

    private static ProxyMethodInvocation stubInvocation(Method method, Object target) {
        ProxyMethodInvocation invocation = Mockito.mock(ProxyMethodInvocation.class);
        Mockito.when(invocation.getMethod()).thenReturn(method);
        Mockito.when(invocation.getThis()).thenReturn(target);
        Mockito.when(invocation.getArguments()).thenReturn(new Object[0]);
        return invocation;
    }

    private static void stubProceed(ProxyMethodInvocation invocation, Object outcome) {
        try {
            if (outcome instanceof Throwable throwable) {
                Mockito.when(invocation.proceed()).thenThrow(throwable);
            } else {
                Mockito.when(invocation.proceed()).thenReturn(outcome);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    static final class CountingTransactionManager implements PlatformTransactionManager {
        private final AtomicInteger beginCount = new AtomicInteger();
        private final AtomicInteger commitCount = new AtomicInteger();
        private final AtomicInteger rollbackCount = new AtomicInteger();

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            beginCount.incrementAndGet();
            return new SimpleTransactionStatus(true);
        }

        @Override
        public void commit(TransactionStatus status) {
            commitCount.incrementAndGet();
        }

        @Override
        public void rollback(TransactionStatus status) {
            rollbackCount.incrementAndGet();
        }

        int beginCount() {
            return beginCount.get();
        }

        int commitCount() {
            return commitCount.get();
        }

        int rollbackCount() {
            return rollbackCount.get();
        }
    }
}
