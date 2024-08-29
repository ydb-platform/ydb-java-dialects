package tech.ydb.slo.hibernate.retry

import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import java.sql.SQLRecoverableException

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@Retryable(
    retryFor = [SQLRecoverableException::class],
    maxAttempts = 5,
    backoff = Backoff(delay = 100, multiplier = 2.0, maxDelay = 5000, random = true),
)
annotation class YdbRetryable