package tech.ydb.exposed.dialect.integration.transaction

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.ydbReadOnlyTransaction
import tech.ydb.exposed.dialect.ydbTransaction
import java.util.concurrent.atomic.AtomicInteger

class YdbRetryingTransactionsIT : BaseYdbTest() {

    object RetryItems : YdbTable("retry_items") {
        val id = integer("id")
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(RetryItems)

    @Test
    fun `ydbTransaction executes a write-and-read round trip`() {
        ydbTransaction(db) {
            RetryItems.insert {
                it[id] = 1
                it[name] = "alpha"
            }
        }

        val name = ydbReadOnlyTransaction(db) {
            RetryItems.selectAll().single()[RetryItems.name]
        }
        assertEquals("alpha", name)
    }

    @Test
    fun `ydbTransaction retries the body on a retryable failure`() {
        val attempts = AtomicInteger()

        ydbTransaction(db, idempotent = true, maxAttempts = 3) {
            val attempt = attempts.incrementAndGet()
            if (attempt < 2) {
                throw FakeAbortedException()
            }
            RetryItems.insert {
                it[id] = 42
                it[name] = "retried"
            }
        }

        assertEquals(2, attempts.get())

        val stored = ydbReadOnlyTransaction(db) {
            RetryItems.selectAll().single()[RetryItems.name]
        }
        assertEquals("retried", stored)
    }

    @Test
    fun `non-retryable error fails fast`() {
        val attempts = AtomicInteger()

        assertThrows(IllegalStateException::class.java) {
            ydbTransaction(db, maxAttempts = 3) {
                attempts.incrementAndGet()
                error("non-retryable")
            }
        }
        assertEquals(1, attempts.get())
    }

    private class FakeAbortedException : RuntimeException("simulated"), tech.ydb.jdbc.exception.YdbStatusable {
        override fun getStatus(): tech.ydb.core.Status =
            tech.ydb.core.Status.of(tech.ydb.core.StatusCode.ABORTED)
    }
}
