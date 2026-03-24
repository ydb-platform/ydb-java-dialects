package tech.ydb.exposed.dialect.transaction

import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.SQLException
import kotlin.math.min

private val retryableErrors = listOf(
    "ABORTED",
    "UNAVAILABLE",
    "OVERLOADED"
)

fun <T> ydbTransaction(
    maxRetries: Int = 5,
    initialBackoffMs: Long = 50,
    maxBackoffMs: Long = 2000,
    block: Transaction.() -> T
): T {

    var attempt = 0
    var backoff = initialBackoffMs
    var lastException: Exception? = null

    while (attempt < maxRetries) {

        try {
            return transaction {
                block()
            }

        } catch (e: SQLException) {

            val message = e.message ?: ""

            val retryable = retryableErrors.any {
                message.contains(it, ignoreCase = true)
            }

            if (!retryable) {
                throw e
            }

            lastException = e
        }

        Thread.sleep(backoff)

        backoff = min(backoff * 2, maxBackoffMs)
        attempt++
    }

    throw lastException ?: IllegalStateException("Transaction failed after retries")
}