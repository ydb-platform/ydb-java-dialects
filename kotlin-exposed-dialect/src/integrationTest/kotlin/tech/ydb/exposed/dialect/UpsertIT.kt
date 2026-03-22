package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UpsertIT : BaseYdbTest() {

    object Products : Table("products") {
        val id = integer("id")
        val name = varchar("name", 255)
        override val primaryKey = PrimaryKey(id)
    }

    @Test
    fun `should perform UPSERT`() = tx {
        SchemaUtils.create(Products)

        // Используем YdbFunctionProvider.upsert
        val provider = YdbFunctionProvider()
        val data = listOf(
            Products.id to 1,
            Products.name to "Item1"
        )

        provider.upsert(
            table = Products,
            data = data,
            expression = "",
            onUpdate = emptyList(),
            keyColumns = listOf(Products.id),
            where = null,
            transaction = this
        )

        // Проверяем результат
        val row = Products.selectAll().single()
        assertEquals("Item1", row[Products.name])
    }
}