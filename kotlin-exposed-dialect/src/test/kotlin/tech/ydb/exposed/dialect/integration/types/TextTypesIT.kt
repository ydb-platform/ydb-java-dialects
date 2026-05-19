package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class TextTypesIT : BaseYdbTest() {

    object TextTypes : Table("text_types") {
        val id = integer("id")
        val varcharCol = varchar("varchar_col", 255)
        val textCol = text("text_col")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(TextTypes)

    @Test
    fun `should round-trip text and varchar values`() = tx {
        TextTypes.insert {
            it[id] = 1
            it[varcharCol] = "hello"
            it[textCol] = "world"
        }

        val row = TextTypes.selectAll().single()

        assertEquals("hello", row[TextTypes.varcharCol])
        assertEquals("world", row[TextTypes.textCol])
    }

    @Test
    fun `should generate ddl for text-based string types`() = tx {
        val ddl = TextTypes.ddl.joinToString(" ")

        assertTrue(ddl.contains("varchar_col Text"))
        assertTrue(ddl.contains("text_col Text"))
    }
}
