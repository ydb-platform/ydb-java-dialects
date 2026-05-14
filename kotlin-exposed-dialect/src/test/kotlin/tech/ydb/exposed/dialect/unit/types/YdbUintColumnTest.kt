package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.types.YdbUint64ColumnType

class YdbUintColumnTest {

    private val type = YdbUint64ColumnType()

    @Test
    fun `should return uint64 sql type`() {
        assertEquals("Uint64", type.sqlType())
    }

    @Test
    fun `should accept non negative values`() {
        assertEquals(123L, type.notNullValueToDB(123L))
        assertEquals("123", type.nonNullValueToString(123L))
    }

    @Test
    fun `should reject negative values`() {
        assertThrows(IllegalArgumentException::class.java) {
            type.notNullValueToDB(-1L)
        }

        assertThrows(IllegalArgumentException::class.java) {
            type.nonNullValueToString(-1L)
        }
    }

    @Test
    fun `should parse values from db`() {
        assertEquals(42L, type.valueFromDB(42L))
        assertEquals(42L, type.valueFromDB(42))
        assertEquals(42L, type.valueFromDB("42"))
    }
}