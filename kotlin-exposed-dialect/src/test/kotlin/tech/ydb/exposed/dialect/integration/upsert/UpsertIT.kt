package tech.ydb.exposed.dialect.integration.upsert

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.functions.YdbFunctionProvider
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class UpsertIT : BaseYdbTest() {

    object Products : YdbTable("products") {
        val id = integer("id")
        val name = varchar("name", 255)
        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Products)

    @Test
    fun `should perform UPSERT`() = tx {
        SchemaUtils.create(Products)

        val provider = YdbFunctionProvider()
        val data = listOf(
            Products.id to 1,
            Products.name to "Item1"
        )

        val sql = provider.upsert(
            table = Products,
            data = data,
            expression = "",
            onUpdate = emptyList(),
            keyColumns = listOf(Products.id),
            where = null,
            transaction = this
        )

        exec(sql)

        val row = Products.selectAll().single()
        Assertions.assertEquals("Item1", row[Products.name])
    }

    @Test
    fun `should perform UPSERT through Exposed DSL`() = tx {
        Products.upsert {
            it[id] = 1
            it[name] = "Item1"
        }

        Products.upsert {
            it[id] = 1
            it[name] = "Item2"
        }

        val row = Products.selectAll().single()
        Assertions.assertEquals("Item2", row[Products.name])
    }
}
