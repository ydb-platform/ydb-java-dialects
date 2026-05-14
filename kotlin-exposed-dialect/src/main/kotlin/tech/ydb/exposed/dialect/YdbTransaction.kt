package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import tech.ydb.core.StatusCode
import tech.ydb.jdbc.exception.YdbStatusable
import kotlin.math.min
import kotlin.random.Random

/**
 * Runs [statement] inside an Exposed [transaction] and retries it on retryable YDB errors
 * (ABORTED / OVERLOADED / BAD_SESSION / TRANSPORT_UNAVAILABLE / ...).
 *
 * This is the recommended way to execute a unit of work against YDB from Kotlin Exposed.
 * YDB uses Optimistic Concurrency Control, so under contention a transaction can fail with
 * `Transaction locks invalidated` — the retry loop here makes such conflicts transparent.
 *
 * Set [idempotent] to `true` if the transaction body has no externally observable side effects
 * besides the database write (e.g. read-only queries, single UPSERT/REPLACE, idempotent writes).
 * Errors with undetermined outcome (TIMEOUT / UNDETERMINED) are retried only when [idempotent] is `true`.
 */
fun <T> ydbTransaction(
    db: Database? = null,
    idempotent: Boolean = false,
    maxAttempts: Int = 5,
    readOnly: Boolean = false,
    statement: JdbcTransaction.() -> T
): T {
    require(maxAttempts >= 1) { "maxAttempts must be >= 1" }

    var lastError: Throwable? = null

    repeat(maxAttempts) { index ->
        val attempt = index + 1
        try {
            return transaction(
                db = db,
                transactionIsolation = java.sql.Connection.TRANSACTION_SERIALIZABLE,
                readOnly = readOnly,
                statement = statement
            )
        } catch (t: Throwable) {
            lastError = t

            val decision = classifyYdbError(t, idempotent)
            if (!decision.retryable || attempt >= maxAttempts) {
                throw t
            }

            val sleepMs = backoffMillis(decision.backoffKind, attempt)
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw e
                }
            }
        }
    }

    throw lastError ?: IllegalStateException("Retry loop finished without result")
}

/**
 * Shortcut for read-only retryable transactions. Reads in YDB are inherently idempotent,
 * so timeouts and undetermined outcomes are also retried.
 */
fun <T> ydbReadOnlyTransaction(
    db: Database? = null,
    maxAttempts: Int = 5,
    statement: JdbcTransaction.() -> T
): T = ydbTransaction(
    db = db,
    idempotent = true,
    maxAttempts = maxAttempts,
    readOnly = true,
    statement = statement
)

internal enum class YdbBackoffKind {
    NONE,
    INSTANT,
    FAST,
    SLOW
}

internal data class YdbRetryDecision(
    val retryable: Boolean,
    val backoffKind: YdbBackoffKind = YdbBackoffKind.NONE
)

internal fun classifyYdbError(error: Throwable, idempotent: Boolean): YdbRetryDecision {
    val code = extractStatusCode(error) ?: return YdbRetryDecision(retryable = false)

    return when (code) {
        StatusCode.ABORTED,
        StatusCode.UNAVAILABLE,
        StatusCode.TRANSPORT_UNAVAILABLE,
        StatusCode.CLIENT_DISCOVERY_FAILED,
        StatusCode.CLIENT_GRPC_ERROR,
        StatusCode.CLIENT_INTERNAL_ERROR ->
            YdbRetryDecision(retryable = true, backoffKind = YdbBackoffKind.FAST)

        StatusCode.OVERLOADED,
        StatusCode.CLIENT_RESOURCE_EXHAUSTED,
        StatusCode.CLIENT_LIMITS_REACHED ->
            YdbRetryDecision(retryable = true, backoffKind = YdbBackoffKind.SLOW)

        StatusCode.BAD_SESSION,
        StatusCode.SESSION_EXPIRED ->
            YdbRetryDecision(retryable = true, backoffKind = YdbBackoffKind.INSTANT)

        StatusCode.SESSION_BUSY ->
            YdbRetryDecision(retryable = true, backoffKind = YdbBackoffKind.FAST)

        StatusCode.TIMEOUT,
        StatusCode.CLIENT_DEADLINE_EXCEEDED,
        StatusCode.CLIENT_DEADLINE_EXPIRED ->
            YdbRetryDecision(
                retryable = idempotent,
                backoffKind = if (idempotent) YdbBackoffKind.INSTANT else YdbBackoffKind.NONE
            )

        StatusCode.UNDETERMINED ->
            YdbRetryDecision(
                retryable = idempotent,
                backoffKind = if (idempotent) YdbBackoffKind.FAST else YdbBackoffKind.NONE
            )

        else -> YdbRetryDecision(retryable = false)
    }
}

private fun extractStatusCode(error: Throwable): StatusCode? {
    var current: Throwable? = error
    while (current != null) {
        if (current is YdbStatusable) {
            return current.status.code
        }
        current = current.cause
    }
    return null
}

internal fun backoffMillis(kind: YdbBackoffKind, attempt: Int): Long {
    val n = attempt.coerceAtLeast(1)
    return when (kind) {
        YdbBackoffKind.NONE -> 0L
        YdbBackoffKind.INSTANT -> 0L
        YdbBackoffKind.FAST -> {
            val base = 25L * (1L shl min(n - 1, 5))
            jitter(base, 15)
        }
        YdbBackoffKind.SLOW -> {
            val base = 200L * (1L shl min(n - 1, 4))
            jitter(base, 50)
        }
    }
}

private fun jitter(base: Long, spreadPercent: Int): Long {
    if (base <= 0) return 0L
    val spread = (base * spreadPercent) / 100
    return base + Random.nextLong(-spread, spread + 1)
}
