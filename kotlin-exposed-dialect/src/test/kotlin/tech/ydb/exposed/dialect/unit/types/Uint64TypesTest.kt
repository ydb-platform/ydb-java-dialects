package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.types.ydbUint64

class Uint64TypesTest : BaseYdbTest() {

    object Uint64Types : YdbTable("uint64_types") {
        val id = integer("id")
        val valueCol = ydbUint64("value_col")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Uint64Types)

    @Test
    fun `should round-trip uint64 type`() = tx {
        Uint64Types.insert {
            it[id] = 1
            it[valueCol] = 1_700_000_000L
        }

        val row = Uint64Types.selectAll().single()
        assertEquals(1_700_000_000L, row[Uint64Types.valueCol])
    }

    @Test
    fun `should generate ddl for uint64 type`() = tx {
        val ddl = Uint64Types.ddl.joinToString(" ")
        assertTrue(ddl.contains("value_col Uint64"))
    }
}