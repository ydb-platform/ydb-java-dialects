package tech.ydb.exposed.dialect.integration.upsert

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

/** [Table.batchUpsert] over native YDB UPSERT. */
class TableBatchUpsertIT : BaseYdbTest() {

    object BatchItems : Table("table_batch_upsert_items") {
        val id = integer("id")
        val name = varchar("name", 255)
        val quantity = integer("quantity")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(BatchItems)

    data class Row(val id: Int, val name: String, val quantity: Int)

    @Test
    fun `Table batchUpsert inserts and updates rows`() = tx {
        BatchItems.batchUpsert(
            listOf(
                Row(1, "apple", 10),
                Row(2, "banana", 20),
            )
        ) { row ->
            this[BatchItems.id] = row.id
            this[BatchItems.name] = row.name
            this[BatchItems.quantity] = row.quantity
        }

        BatchItems.batchUpsert(
            listOf(
                Row(1, "apple-updated", 11),
                Row(3, "orange", 30),
            )
        ) { row ->
            this[BatchItems.id] = row.id
            this[BatchItems.name] = row.name
            this[BatchItems.quantity] = row.quantity
        }

        val rows = BatchItems
            .selectAll()
            .orderBy(BatchItems.id to SortOrder.ASC)
            .toList()

        assertEquals(3, rows.size)
        assertEquals("apple-updated", rows[0][BatchItems.name])
        assertEquals(11, rows[0][BatchItems.quantity])
        assertEquals("banana", rows[1][BatchItems.name])
        assertEquals("orange", rows[2][BatchItems.name])
    }
}
