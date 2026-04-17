package tech.ydb.exposed.dialect.transaction

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection

enum class YdbTransactionMode {
    READ_WRITE,
    READ_ONLY
}

object YdbRetryingTransactions {

    fun <T> withRetry(
        db: Database,
        mode: YdbTransactionMode = YdbTransactionMode.READ_WRITE,
        maxAttempts: Int = 5,
        idempotent: Boolean = mode == YdbTransactionMode.READ_ONLY,
        block: JdbcTransaction.() -> T
    ): T {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }

        var lastError: Throwable? = null

        repeat(maxAttempts) { index ->
            val attempt = index + 1
            try {
                return transaction(
                    db = db,
                    transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
                    readOnly = mode == YdbTransactionMode.READ_ONLY
                ) {
                    block()
                }
            } catch (t: Throwable) {
                lastError = t

                val decision = YdbRetryClassifier.classify(t, idempotent)
                if (!decision.retryable || attempt >= maxAttempts) {
                    throw t
                }

                if (decision.recreateSession) {
                    runCatching { TransactionManager.closeAndUnregister(db) }
                }

                val sleepMs = YdbRetryClassifier.backoffMillis(decision.backoffKind, attempt)
                if (sleepMs > 0) {
                    Thread.sleep(sleepMs)
                }
            }
        }

        throw lastError ?: IllegalStateException("Retry loop finished without result")
    }

    fun <T> readOnly(
        db: Database,
        maxAttempts: Int = 5,
        block: JdbcTransaction.() -> T
    ): T = withRetry(
        db = db,
        mode = YdbTransactionMode.READ_ONLY,
        maxAttempts = maxAttempts,
        idempotent = true,
        block = block
    )

    fun <T> readWrite(
        db: Database,
        maxAttempts: Int = 5,
        idempotent: Boolean = false,
        block: JdbcTransaction.() -> T
    ): T = withRetry(
        db = db,
        mode = YdbTransactionMode.READ_WRITE,
        maxAttempts = maxAttempts,
        idempotent = idempotent,
        block = block
    )
}