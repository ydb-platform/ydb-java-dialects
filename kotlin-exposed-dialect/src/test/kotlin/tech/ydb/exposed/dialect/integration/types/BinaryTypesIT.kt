package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class BinaryTypesIT : BaseYdbTest() {

    object BinaryTypes : YdbTable("binary_types") {
        val id = integer("id")
        val payload = binary("payload")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(BinaryTypes)

    @Test
    fun `should round-trip binary data`() = tx {
        val bytes = byteArrayOf(1, 2, 3, 4)

        BinaryTypes.insert {
            it[id] = 1
            it[payload] = bytes
        }

        val row = BinaryTypes.selectAll().single()
        assertArrayEquals(bytes, row[BinaryTypes.payload])
    }

    @Test
    fun `should generate ddl for binary type`() = tx {
        val ddl = BinaryTypes.ddl.joinToString(" ")
        assertTrue(ddl.contains("payload String"))
    }
}