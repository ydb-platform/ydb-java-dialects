package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Runs [statement] inside an Exposed [transaction] and retries it on retryable YDB errors.
 *
 * Retry behaviour is controlled by [retry] ([YdbRetryConfig]), aligned with
 * [YdbRetryPolicy](https://github.com/ydb-platform/ydb-dotnet-sdk/blob/main/src/Ydb.Sdk/src/Ado/RetryPolicy/YdbRetryPolicy.cs).
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
