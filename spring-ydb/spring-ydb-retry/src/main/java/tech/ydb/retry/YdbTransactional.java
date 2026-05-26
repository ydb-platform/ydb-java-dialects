package tech.ydb.retry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extends the standard {@code @Transactional} annotation
 * with YDB-specific retry settings for re-executing a transactional method
 * when a retryable YDB error occurs.
 * Can be used with {@code @Transactional} in the same application.
 */

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Transactional
public @interface YdbTransactional {

    @AliasFor(annotation = Transactional.class, attribute = "value")
    String value() default "";

    @AliasFor(annotation = Transactional.class, attribute = "transactionManager")
    String transactionManager() default "";

    @AliasFor(annotation = Transactional.class, attribute = "label")
    String[] label() default {};

    @AliasFor(annotation = Transactional.class, attribute = "propagation")
    Propagation propagation() default Propagation.REQUIRED;

    @AliasFor(annotation = Transactional.class, attribute = "isolation")
    Isolation isolation() default Isolation.DEFAULT;

    @AliasFor(annotation = Transactional.class, attribute = "timeout")
    int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;

    @AliasFor(annotation = Transactional.class, attribute = "timeoutString")
    String timeoutString() default "";

    @AliasFor(annotation = Transactional.class, attribute = "readOnly")
    boolean readOnly() default false;

    @AliasFor(annotation = Transactional.class, attribute = "rollbackFor")
    Class<? extends Throwable>[] rollbackFor() default {};

    @AliasFor(annotation = Transactional.class, attribute = "rollbackForClassName")
    String[] rollbackForClassName() default {};

    @AliasFor(annotation = Transactional.class, attribute = "noRollbackFor")
    Class<? extends Throwable>[] noRollbackFor() default {};

    @AliasFor(annotation = Transactional.class, attribute = "noRollbackForClassName")
    String[] noRollbackForClassName() default {};

    /**
     * Enables or disables YDB retry for the annotated scope.
     * This flag affects retry behavior only and does not disable transactional execution.
     * Retry can be disabled for the annotated scope, but it cannot be enabled here if it is disabled in the global configuration.
     */
    boolean enabled() default true;

    /**
     * Specifies the maximum number of retry attempts after the initial failed execution.
     * The annotated method may be executed up to {@code maxRetries + 1} times in total.
     * Use {@code -1} to inherit the value from the global retry configuration.
     * A value of {@code 0} is not supported; use {@code enabled() = false} to disable retry.
     */
    int maxRetries() default -1;

    /**
     * Overrides the base delay in milliseconds for the slow backoff strategy.
     * Use {@code -1} to inherit the value from the global retry configuration.
     */
    int slowBackoffBaseMs() default -1;

    /**
     * Overrides the base delay in milliseconds for the fast backoff strategy.
     * Use {@code -1} to inherit the value from the global retry configuration.
     */
    int fastBackoffBaseMs() default -1;

    /**
     * Overrides the maximum delay in milliseconds for the slow backoff strategy.
     * Use {@code -1} to inherit the value from the global retry configuration.
     */
    int slowCapBackoffMs() default -1;

    /**
     * Overrides the maximum delay in milliseconds for the fast backoff strategy.
     * Use {@code -1} to inherit the value from the global retry configuration.
     */
    int fastCapBackoffMs() default -1;

    /**
     * Marks the transactional method as idempotent for YDB retries.
     * Some YDB errors are retryable only for idempotent operations.
     */
    boolean idempotent() default false;
}
