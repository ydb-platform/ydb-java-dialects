package tech.ydb.retry;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.ydb.core.StatusCode.ABORTED;
import static tech.ydb.core.StatusCode.BAD_SESSION;

/**
 * Locks in the contract that when one {@code @YdbTransactional} method invokes another
 * {@code @YdbTransactional} method through a Spring proxy with {@link
 * org.springframework.transaction.annotation.Propagation#REQUIRED REQUIRED} propagation, only the
 * outer (root) transaction is retried as a whole and the inner method does <strong>not</strong>
 * start its own retry loop.
 *
 * <p>This is the "если пропагет транзакция, то не ретраим вложенную" invariant from the PR
 * description. Without this property, an outer rollback would still spend retries on an inner
 * call, which is both pointless (the outer tx is already doomed) and would break OCC semantics in
 * YDB.
 */
class NestedYdbTransactionalRetryTest {

    @Test
    void shouldRetryOnlyOuterMethodWhenInnerJoinsExistingTransaction() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(NestedConfig.class)) {
            NestedService service = context.getBean(NestedService.class);
            FlakyTransactionManager txManager = context.getBean(FlakyTransactionManager.class);

            service.outer();

            assertEquals(3, service.outerCount(), "outer must be re-invoked from scratch on retry");
            assertEquals(3, service.innerCount(),
                    "inner is called once per outer attempt and must NOT add its own retry attempts");
            assertEquals(3, txManager.beginCount(),
                    "only the outer attempts begin physical transactions (REQUIRED joins)");
            assertEquals(2, txManager.rollbackCount());
            assertEquals(1, txManager.commitCount());
        }
    }

    @Test
    void shouldNotRetryInnerWhenOuterFails() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(NestedConfig.class)) {
            NestedService service = context.getBean(NestedService.class);
            FlakyTransactionManager txManager = context.getBean(FlakyTransactionManager.class);

            service.failingInnerCallCounter().set(Integer.MAX_VALUE);

            assertThrows(ConfigurableInnerException.class, service::outerWithFailingInner);

            assertEquals(1, service.outerCount(),
                    "outer must not be retried for non-YDB inner failures");
            assertEquals(1, service.innerCount(),
                    "inner must be invoked exactly once");
            assertEquals(1, txManager.beginCount());
            assertEquals(1, txManager.rollbackCount());
            assertEquals(0, txManager.commitCount());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @Import(YdbTransactionAutoConfiguration.class)
    static class NestedConfig {
        @Bean
        FlakyTransactionManager flakyTransactionManager() {
            return new FlakyTransactionManager();
        }

        @Bean
        NestedService nestedService() {
            return new NestedService();
        }
    }

    /** Spring service whose outer method calls the inner method through the proxy. */
    static class NestedService {
        private final AtomicInteger outerCount = new AtomicInteger();
        private final AtomicInteger innerCount = new AtomicInteger();
        private final AtomicInteger failingInnerCallCounter = new AtomicInteger();

        private NestedService self;

        @org.springframework.beans.factory.annotation.Autowired
        void setSelf(NestedService self) {
            this.self = self;
        }

        @YdbTransactional(maxRetries = 5)
        public void outer() {
            outerCount.incrementAndGet();
            self.inner();
        }

        @YdbTransactional(maxRetries = 5)
        public void inner() {
            innerCount.incrementAndGet();
            if (innerCount.get() == 1) {
                throw new ConfigurableStatusException(ABORTED);
            }
            if (innerCount.get() == 2) {
                throw new ConfigurableStatusException(BAD_SESSION);
            }
        }

        @YdbTransactional(maxRetries = 5)
        public void outerWithFailingInner() {
            outerCount.incrementAndGet();
            self.failingInner();
        }

        @YdbTransactional(maxRetries = 5)
        public void failingInner() {
            innerCount.incrementAndGet();
            throw new ConfigurableInnerException();
        }

        AtomicInteger failingInnerCallCounter() {
            return failingInnerCallCounter;
        }

        int outerCount() {
            return outerCount.get();
        }

        int innerCount() {
            return innerCount.get();
        }
    }

    /**
     * Transaction manager that counts physical begin/commit/rollback calls and supports
     * propagation-REQUIRED "join existing transaction" semantics via a thread-local TxObject with a
     * rollback-only flag.
     */
    static final class FlakyTransactionManager extends AbstractPlatformTransactionManager {
        private final AtomicInteger beginCount = new AtomicInteger();
        private final AtomicInteger commitCount = new AtomicInteger();
        private final AtomicInteger rollbackCount = new AtomicInteger();
        private final ThreadLocal<TxObject> current = new ThreadLocal<>();

        FlakyTransactionManager() {
            setNestedTransactionAllowed(true);
        }

        @Override
        protected Object doGetTransaction() {
            TxObject existing = current.get();
            if (existing != null) {
                return existing;
            }
            return new TxObject();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return ((TxObject) transaction).active;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            beginCount.incrementAndGet();
            TxObject tx = (TxObject) transaction;
            tx.active = true;
            current.set(tx);
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commitCount.incrementAndGet();
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbackCount.incrementAndGet();
        }

        @Override
        protected void doSetRollbackOnly(DefaultTransactionStatus status) {
            // No-op: tests only assert begin/commit/rollback counts; we just need this hook to
            // exist so Spring does not throw IllegalTransactionStateException for participating
            // transactions in propagation REQUIRED.
        }

        @Override
        protected void doCleanupAfterCompletion(Object transaction) {
            ((TxObject) transaction).active = false;
            current.remove();
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

        private static final class TxObject {
            boolean active;
        }
    }

    static final class ConfigurableStatusException extends RuntimeException {
        ConfigurableStatusException(tech.ydb.core.StatusCode statusCode) {
            super(new java.sql.SQLException(
                    "test", null, statusCode.getCode()));
        }
    }

    static final class ConfigurableInnerException extends RuntimeException {
    }
}
