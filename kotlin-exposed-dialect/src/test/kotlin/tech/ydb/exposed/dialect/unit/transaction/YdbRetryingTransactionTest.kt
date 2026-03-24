package tech.ydb.exposed.dialect.unit.transaction

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.transaction.ydbTransaction
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicInteger

class YdbRetryingTransactionTest {
    @Test
    fun `should retry aborted transaction`() {

        val attempts = AtomicInteger(0)

        val result = try {
            ydbTransaction(maxRetries = 3) {

                if (attempts.incrementAndGet() < 3) {
                    throw SQLException("ABORTED")
                }

                42
            }
        } catch (e: Exception) {
            -1
        }

        Assertions.assertEquals(42, result)
        Assertions.assertEquals(3, attempts.get())
    }

    @Test
    fun `should not retry non retryable errors`() {

        var attempts = 0

        try {
            ydbTransaction(maxRetries = 3) {

                attempts++
                throw SQLException("SYNTAX ERROR")

            }
        } catch (_: SQLException) {
        }

        Assertions.assertEquals(1, attempts)
    }
}