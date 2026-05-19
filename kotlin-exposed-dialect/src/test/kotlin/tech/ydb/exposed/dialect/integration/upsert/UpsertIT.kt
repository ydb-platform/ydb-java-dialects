package tech.ydb.exposed.dialect.integration.upsert

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.replace
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class UpsertIT : BaseYdbTest() {

    object Products : Table("products") {
        val id = integer("id")
        val name = varchar("name", 255)
        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Products)

    @Test
    fun `Table upsert inserts and then updates the same row`() = tx {
        Products.upsert {
            it[id] = 1
            it[name] = "Item1"
        }

        Products.upsert {
            it[id] = 1
            it[name] = "Item2"
        }

        val row = Products.selectAll().single()
        assertEquals("Item2", row[Products.name])
    }

    @Test
    fun `Table replace overwrites an existing row by primary key`() = tx {
        Products.upsert {
            it[id] = 5
            it[name] = "original"
        }

        Products.replace {
            it[id] = 5
            it[name] = "replaced"
        }

        val row = Products.selectAll().single()
        assertEquals(5, row[Products.id])
        assertEquals("replaced", row[Products.name])
    }
}
