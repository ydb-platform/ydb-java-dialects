package tech.ydb.exposed.dialect.integration.batch

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class BatchOperationsIT : BaseYdbTest() {

    object BatchItems : YdbTable("batch_items") {
        val id = integer("id")
        val name = varchar("name", 255)
        val quantity = integer("quantity")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(BatchItems)

    data class ItemRow(
        val id: Int,
        val name: String,
        val quantity: Int
    )

    @Test
    fun `should support batch insert`() = tx {
        val items = listOf(
            ItemRow(1, "apple", 10),
            ItemRow(2, "banana", 20),
            ItemRow(3, "orange", 30)
        )

        BatchItems.batchInsert(items) { item ->
            this[BatchItems.id] = item.id
            this[BatchItems.name] = item.name
            this[BatchItems.quantity] = item.quantity
        }

        val rows = BatchItems
            .selectAll()
            .orderBy(BatchItems.id to SortOrder.ASC)
            .toList()

        assertEquals(3, rows.size)
        assertEquals("apple", rows[0][BatchItems.name])
        assertEquals(10, rows[0][BatchItems.quantity])
        assertEquals("banana", rows[1][BatchItems.name])
        assertEquals(20, rows[1][BatchItems.quantity])
        assertEquals("orange", rows[2][BatchItems.name])
        assertEquals(30, rows[2][BatchItems.quantity])
    }

    @Test
    fun `should allow querying rows inserted by batch insert`() = tx {
        val items = listOf(
            ItemRow(1, "apple", 10),
            ItemRow(2, "banana", 20),
            ItemRow(3, "orange", 30)
        )

        BatchItems.batchInsert(items) { item ->
            this[BatchItems.id] = item.id
            this[BatchItems.name] = item.name
            this[BatchItems.quantity] = item.quantity
        }

        val banana = BatchItems
            .selectAll()
            .where { BatchItems.name eq "banana" }
            .single()

        assertEquals(2, banana[BatchItems.id])
        assertEquals(20, banana[BatchItems.quantity])
        assertTrue(banana[BatchItems.name].startsWith("ban"))
    }
}