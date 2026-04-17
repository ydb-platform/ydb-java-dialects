package tech.ydb.exposed.dialect.integration.query

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class SubqueryIT : BaseYdbTest() {

    object Sales : YdbTable("sales") {
        val id = integer("id")
        val customer = varchar("customer", 255)
        val amount = integer("amount")
        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Sales)

    @Test
    fun `should support selecting from aliased subquery`() = tx {
        Sales.insert {
            it[id] = 1
            it[customer] = "Alice"
            it[amount] = 100
        }
        Sales.insert {
            it[id] = 2
            it[customer] = "Bob"
            it[amount] = 200
        }
        Sales.insert {
            it[id] = 3
            it[customer] = "Carol"
            it[amount] = 300
        }

        val filtered = Sales
            .select(Sales.id, Sales.customer, Sales.amount)
            .where { Sales.amount greaterEq 200 }

        val base = filtered.alias("sales_filtered")

        val idCol = base[Sales.id]
        val customerCol = base[Sales.customer]
        val amountCol = base[Sales.amount]

        val rows = base
            .select(idCol, customerCol, amountCol)
            .orderBy(idCol to SortOrder.ASC)
            .toList()

        assertEquals(2, rows.size)
        assertEquals(2, rows[0][idCol])
        assertEquals("Bob", rows[0][customerCol])
        assertEquals(200, rows[0][amountCol])

        assertEquals(3, rows[1][idCol])
        assertEquals("Carol", rows[1][customerCol])
        assertEquals(300, rows[1][amountCol])
    }
}