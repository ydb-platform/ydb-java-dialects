package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Runs [statement] inside an Exposed [transaction] with YDB-friendly defaults and retries.
 *
 * Each attempt uses `TRANSACTION_SERIALIZABLE` (YDB snapshot isolation / OCC). On retryable
 * [java.sql.SQLException] vendor codes ([getNextRetryDelayMs]) the whole transaction is re-run after
 * jittered backoff; the last failure is rethrown when [YdbRetryConfig.maxAttempts] is exhausted.
 *
 * Use [YdbRetryConfig.IDEMPOTENT] only when the body is safe to repeat (reads, idempotent UPSERT).
 *
 * @param db Target database; `null` uses Exposed's current default.
 * @param retry Backoff caps and whether non-transient codes may be retried.
 * @param readOnly Passed through to Exposed `transaction(readOnly = ...)`.
 */
fun <T> ydbTransaction(
    db: Database? = null,
    retry: YdbRetryConfig = YdbRetryConfig.DEFAULT,
    readOnly: Boolean = false,
    statement: JdbcTransaction.() -> T
): T {
    require(retry.maxAttempts >= 1) { "maxAttempts must be >= 1" }

    var lastError: Throwable? = null

    repeat(retry.maxAttempts) { attempt ->
        try {
            return transaction(
                db = db,
                transactionIsolation = java.sql.Connection.TRANSACTION_SERIALIZABLE,
                readOnly = readOnly,
                statement = statement
            )
        } catch (t: Throwable) {
            lastError = t

            val delayMs = getNextRetryDelayMs(t, attempt, retry) ?: throw t

            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw e
                }
            }
        }
    }

    throw lastError ?: IllegalStateException("Retry loop finished without result")
}
