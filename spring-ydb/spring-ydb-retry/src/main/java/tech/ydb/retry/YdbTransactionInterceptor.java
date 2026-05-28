package tech.ydb.retry;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.OptionalLong;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class YdbTransactionInterceptor extends TransactionInterceptor {

    private static final Logger log = LoggerFactory.getLogger(YdbTransactionInterceptor.class);
    private final YdbRetryPolicyConfig retryConfig;
    private final BackoffSleeper backoffSleeper;

    public YdbTransactionInterceptor() {
        this(new YdbRetryPolicyConfig(), Thread::sleep);
    }

    YdbTransactionInterceptor(YdbRetryPolicyConfig retryConfig, BackoffSleeper backoffSleeper) {
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
            if (log.isDebugEnabled()) {
                log.debug("YDB retry is disabled for method "
                        + invocation.getMethod().toGenericString()
                        + " because it participates in an existing transaction");
            }
            return this.invokeWithinTransaction(invocation.getMethod(), targetClass, createCallback(invocation));
        }

        YdbTransactional ydbTransactional = resolveYdbTransactionAnnotation(invocation.getMethod(), targetClass);
        YdbRetryPolicyConfig effectiveConfig = this.retryConfig.merge(ydbTransactional);
        boolean isIdempotent = ydbTransactional != null && ydbTransactional.idempotent();

        if (!effectiveConfig.isEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("YDB retry is disabled for method "
                        + invocation.getMethod().toGenericString());
            }
            return this.invokeWithinTransaction(invocation.getMethod(), targetClass, createCallback(invocation));
        }

        return invokeWithinTransactionWithRetryContext(invocation, targetClass, effectiveConfig, isIdempotent);
    }

    @Nullable
    private Object invokeWithinTransactionWithRetryContext(
            final MethodInvocation invocation,
            @Nullable Class<?> targetClass,
            YdbRetryPolicyConfig effectiveConfig,
            boolean isIdempotent)
            throws Throwable {
        for (int attempt = 0; ; attempt++) {
            try {
                MethodInvocation cloneInvocation = cloneInvocation(invocation);
                return this.invokeWithinTransaction(
                        invocation.getMethod(), targetClass, createCallback(cloneInvocation));
            } catch (Throwable ex) {
                if (ex instanceof Error) {
                    throw ex;
                }
                int vendorCode = YdbRetryPolicy.extractVendorCode(ex);
                OptionalLong delay =
                        YdbRetryPolicy.getNextRetryDelayMs(vendorCode, attempt, effectiveConfig, isIdempotent);
                if (delay.isEmpty()) {
                    throw ex;
                }
                sleep(delay.getAsLong(), ex);
            }
        }
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
    private YdbTransactional resolveYdbTransactionAnnotation(
            Method method, @Nullable Class<?> targetClass) {
        Method specificMethod =
                targetClass != null ? AopUtils.getMostSpecificMethod(method, targetClass) : method;

        YdbTransactional annotation = findYdbTransactional(specificMethod);
        if (annotation != null) {
            return annotation;
        }

        annotation = findYdbTransactional(targetClass);
        if (annotation != null) {
            return annotation;
        }

        if (!specificMethod.equals(method)) {
            annotation = findYdbTransactional(method);
            if (annotation != null) {
                return annotation;
            }
        }

        return findYdbTransactional(method.getDeclaringClass());
    }

    @Nullable
    private YdbTransactional findYdbTransactional(@Nullable AnnotatedElement element) {
        return element != null
                ? AnnotatedElementUtils.findMergedAnnotation(element, YdbTransactional.class)
                : null;
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

    private MethodInvocation cloneInvocation(MethodInvocation invocation) {
        if (invocation instanceof ProxyMethodInvocation proxyMethodInvocation) {
            return proxyMethodInvocation.invocableClone();
        }
        return invocation;
    }
}
