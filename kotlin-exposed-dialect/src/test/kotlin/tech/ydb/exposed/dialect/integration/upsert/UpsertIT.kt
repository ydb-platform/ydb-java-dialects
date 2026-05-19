package tech.ydb.exposed.dialect.integration.upsert

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.replace
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

/**
 * Integration coverage for Exposed [Table.upsert] / [Table.replace] backed by native YQL.
 *
 * Scenarios mirror the upstream Exposed JDBC upsert tests where YDB semantics allow:
 * - insert-on-miss / update-on-hit via UPSERT
 * - full row overwrite via REPLACE
 * - partial UPSERT (only listed columns change on conflict)
 */
class UpsertIT : BaseYdbTest() {

    object Products : Table("upsert_products") {
        val id = integer("id")
        val name = varchar("name", 255)
        override val primaryKey = PrimaryKey(id)
    }

    object Inventory : Table("upsert_inventory") {
        val id = integer("id")
        val name = varchar("name", 255)
        val quantity = integer("quantity").default(0)
        override val primaryKey = PrimaryKey(id)
    }

    object NullableItems : Table("upsert_nullable_items") {
        val id = integer("id")
        val note = varchar("note", 64).nullable()
        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Products, Inventory, NullableItems)

    @Test
    fun `Table upsert inserts a new row`() = tx {
        Products.upsert {
            it[id] = 1
            it[name] = "new"
        }

        val row = Products.selectAll().single()
        assertEquals(1, row[Products.id])
        assertEquals("new", row[Products.name])
    }

    @Test
    fun `Table upsert updates an existing row by primary key`() = tx {
        Products.upsert {
            it[id] = 1
            it[name] = "first"
        }
        Products.upsert {
            it[id] = 1
            it[name] = "second"
        }

        val row = Products.selectAll().single()
        assertEquals("second", row[Products.name])
    }

    @Test
    fun `Table upsert can insert multiple rows`() = tx {
        Products.upsert {
            it[id] = 1
            it[name] = "a"
        }
        Products.upsert {
            it[id] = 2
            it[name] = "b"
        }
        Products.upsert {
            it[id] = 3
            it[name] = "c"
        }

        val names = Products
            .selectAll()
            .orderBy(Products.id to SortOrder.ASC)
            .map { it[Products.name] }

        assertEquals(listOf("a", "b", "c"), names)
    }

    @Test
    fun `Table upsert on conflict updates only columns listed in the block`() = tx {
        Inventory.upsert {
            it[id] = 10
            it[name] = "widget"
            it[quantity] = 5
        }

        Inventory.upsert {
            it[id] = 10
            it[name] = "renamed"
        }

        val row = Inventory.selectAll().single()
        assertEquals("renamed", row[Inventory.name])
        assertEquals(5, row[Inventory.quantity])
    }

    @Test
    fun `Table replace overwrites the row and resets omitted columns to defaults`() = tx {
        Inventory.upsert {
            it[id] = 20
            it[name] = "before"
            it[quantity] = 7
        }

        Inventory.replace {
            it[id] = 20
            it[name] = "after"
        }

        val row = Inventory.selectAll().single()
        assertEquals(20, row[Inventory.id])
        assertEquals("after", row[Inventory.name])
        assertEquals(0, row[Inventory.quantity])
    }

    @Test
    fun `Table replace overwrites all listed column values`() = tx {
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

    @Test
    fun `Table upsert is idempotent when writing the same values twice`() = tx {
        Products.upsert {
            it[id] = 99
            it[name] = "stable"
        }
        Products.upsert {
            it[id] = 99
            it[name] = "stable"
        }

        val rows = Products.selectAll().where { Products.id eq 99 }.toList()
        assertEquals(1, rows.size)
        assertEquals("stable", rows.single()[Products.name])
    }

    @Test
    fun `Table replace can insert when the primary key is new`() = tx {
        Products.replace {
            it[id] = 42
            it[name] = "via-replace"
        }

        val row = Products.selectAll().single()
        assertEquals(42, row[Products.id])
        assertEquals("via-replace", row[Products.name])
    }

    @Test
    fun `Table upsert leaves other rows untouched`() = tx {
        Products.upsert {
            it[id] = 1
            it[name] = "keep"
        }
        Products.upsert {
            it[id] = 2
            it[name] = "change-me"
        }

        Products.upsert {
            it[id] = 2
            it[name] = "changed"
        }

        val kept = Products.selectAll().where { Products.id eq 1 }.single()
        assertEquals("keep", kept[Products.name])

        val updated = Products.selectAll().where { Products.id eq 2 }.single()
        assertEquals("changed", updated[Products.name])
    }

    @Test
    fun `Table upsert followed by replace yields replace semantics`() = tx {
        Inventory.upsert {
            it[id] = 30
            it[name] = "stock"
            it[quantity] = 100
        }

        Inventory.replace {
            it[id] = 30
            it[name] = "cleared-qty"
        }

        val row = Inventory.selectAll().single()
        assertEquals("cleared-qty", row[Inventory.name])
        assertEquals(0, row[Inventory.quantity])
    }

    @Test
    fun `Table upsert can clear a nullable column when explicitly set to null`() = tx {
        NullableItems.upsert {
            it[id] = 1
            it[note] = "present"
        }
        NullableItems.upsert {
            it[id] = 1
            it[note] = null
        }

        assertNull(NullableItems.selectAll().single()[NullableItems.note])
    }
}
