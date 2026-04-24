package tech.ydb.exposed.dialect.integration.transaction

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.transaction.YdbRetryingTransactions

class YdbRetryingTransactionsIT : BaseYdbTest() {

    object RetryItems : YdbTable("retry_items") {
        val id = integer("id")
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(RetryItems)

    @Test
    fun `should execute read write helper`() {
        YdbRetryingTransactions.readWrite(db) {
            RetryItems.insert {
                it[id] = 1
                it[name] = "alpha"
            }
        }

        YdbRetryingTransactions.readOnly(db) {
            val row = RetryItems.selectAll().single()
            assertEquals("alpha", row[RetryItems.name])
        }
    }
}