package tech.ydb.retry;

import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tech.ydb.core.StatusCode;
import tech.ydb.jdbc.exception.YdbStatusable;

public class YdbTransactionInterceptor extends TransactionInterceptor {

    private static final Logger log = LoggerFactory.getLogger(YdbTransactionInterceptor.class);
    private final YdbRetryPolicyConfig retryConfig;
    private final BackoffSleeper backoffSleeper;

    public YdbTransactionInterceptor() {
        this(new YdbRetryPolicyConfig(), Thread::sleep);
    }

    YdbTransactionInterceptor(YdbRetryPolicyConfig retryConfig,
                              BackoffSleeper backoffSleeper) {
        this.retryConfig = retryConfig;
        this.backoffSleeper = backoffSleeper;
    }

    @Override
    @Nullable
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        Class<?> targetClass = invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null;

        TransactionAttributeSource tas = getTransactionAttributeSource();
        final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(invocation.getMethod(), targetClass) : null);
        if (txAttr == null) {
            return this.invokeWithinTransaction(invocation.getMethod(), targetClass, createCallback(invocation));
        }

        if (isParticipatingInExistingTransaction(txAttr)) {
            log.debug(
                    "YDB retry is disabled for method {} because it participates in an existing transaction",
                    invocation.getMethod().toGenericString()
            );
            return this.invokeWithinTransaction(invocation.getMethod(), targetClass, createCallback(invocation));
        }

        YdbTransactional ydbTransactional = resolveYdbTransactionAnnotation(invocation.getMethod(), targetClass);
        YdbRetryPolicyConfig retryConfig = this.retryConfig.merge(ydbTransactional);

        if (!retryConfig.isEnabled()) {
            log.debug("YDB retry is disabled for method {}", invocation.getMethod().toGenericString());
            return this.invokeWithinTransaction(invocation.getMethod(), targetClass, createCallback(invocation));
        }

        return invokeWithinTransactionWithRetryContext(invocation, targetClass, retryConfig);
    }

    @Nullable
    private Object invokeWithinTransactionWithRetryContext(final MethodInvocation invocation,
                                                           @Nullable Class<?> targetClass,
                                                           YdbRetryPolicyConfig retryConfig) throws Throwable {
        for (int attempt = 1; attempt <= retryConfig.getMaxRetries() + 1; attempt++) {
            try {
                return this.invokeWithinTransaction(invocation.getMethod(), targetClass, createCallback(invocation));
            } catch (Throwable ex) {
                if (ex instanceof Error) {
                    throw ex;
                }
                StatusCode statusCode = extractStatusCode(ex);
                if (!YdbRetryPolicy.shouldRetry(statusCode, retryConfig.isIdempotent())) {
                    throw ex;
                }
                if (attempt == retryConfig.getMaxRetries() + 1) {
                    throw ex;
                }
                long delay = YdbDelayCalculator.calculateDelay(statusCode, retryConfig, attempt - 1);
                sleep(delay, ex);
            }
        }
        throw new IllegalStateException("retry loop finished unexpectedly");
    }

    private void sleep(long delay, Throwable originalException) throws Throwable {
        try {
            backoffSleeper.sleep(delay);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            interruptedException.addSuppressed(originalException);
            throw interruptedException;
        }
    }

    private boolean isParticipatingInExistingTransaction(TransactionAttribute txAttr) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return false;
        }
        int propagationBehavior = txAttr.getPropagationBehavior();

        return propagationBehavior != TransactionDefinition.PROPAGATION_REQUIRES_NEW
                && propagationBehavior != TransactionDefinition.PROPAGATION_NOT_SUPPORTED
                && propagationBehavior != TransactionDefinition.PROPAGATION_NEVER;
    }

    @Nullable
    private YdbTransactional resolveYdbTransactionAnnotation(Method method, @Nullable Class<?> targetClass) {
        Method specificMethod = targetClass != null ? AopUtils.getMostSpecificMethod(method, targetClass) : method;
        YdbTransactional methodLevel = AnnotatedElementUtils.findMergedAnnotation(specificMethod, YdbTransactional.class);
        if (methodLevel != null) {
            return methodLevel;
        }
        if (targetClass != null) {
            return AnnotatedElementUtils.findMergedAnnotation(targetClass, YdbTransactional.class);
        }
        return null;
    }

    @Nullable
    private StatusCode extractStatusCode(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof YdbStatusable statusable && statusable.getStatus() != null) {
                return statusable.getStatus().getCode();
            }
            current = current.getCause();
        }
        return null;
    }

    private InvocationCallback createCallback(MethodInvocation invocation) {
        return new InvocationCallback() {
            @Nullable
            public Object proceedWithInvocation() throws Throwable {
                return invocation.proceed();
            }

            public Object getTarget() {
                return invocation.getThis();
            }

            public Object[] getArguments() {
                return invocation.getArguments();
            }
        };
    }
}
