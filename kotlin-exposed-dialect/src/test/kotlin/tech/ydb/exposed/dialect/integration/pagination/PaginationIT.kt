package tech.ydb.exposed.dialect.integration.pagination

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class PaginationIT : BaseYdbTest() {

    object Items : YdbTable("items") {
        val id = integer("id")
        val name = varchar("name", 255)
        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Items)

    @Test
    fun `should support LIMIT`() = tx {
        Items.insert {
            it[id] = 1
            it[name] = "one"
        }
        Items.insert {
            it[id] = 2
            it[name] = "two"
        }
        Items.insert {
            it[id] = 3
            it[name] = "three"
        }

        val rows = Items.selectAll().limit(2).toList()
        Assertions.assertEquals(2, rows.size)
    }
}