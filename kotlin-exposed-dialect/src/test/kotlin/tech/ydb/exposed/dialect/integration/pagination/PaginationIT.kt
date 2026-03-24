package tech.ydb.exposed.dialect.integration.pagination

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class PaginationIT : BaseYdbTest() {

    object Items : Table("items") {
        val id = integer("id")
        val name = varchar("name", 255)
        override val primaryKey = PrimaryKey(id)
    }

    @Test
    fun `should support LIMIT`() = tx {
        SchemaUtils.create(Items)

        Items.insert { it[id] = 1; it[name] = "A" }
        Items.insert { it[id] = 2; it[name] = "B" }
        Items.insert { it[id] = 3; it[name] = "C" }

        val rows = Items.selectAll().limit(2).toList()
        Assertions.assertEquals(2, rows.size)
    }
}