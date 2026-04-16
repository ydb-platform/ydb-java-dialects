package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.types.YdbDecimalColumnType
import java.math.BigDecimal

class YdbDecimalColumnTypeTest {

    @Test
    fun `should return decimal sql type`() {
        val type = YdbDecimalColumnType(10, 2)
        assertEquals("Decimal(10, 2)", type.sqlType())
    }

    @Test
    fun `should reject invalid precision`() {
        assertThrows(IllegalArgumentException::class.java) {
            YdbDecimalColumnType(0, 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            YdbDecimalColumnType(36, 2)
        }
    }

    @Test
    fun `should reject invalid scale`() {
        assertThrows(IllegalArgumentException::class.java) {
            YdbDecimalColumnType(10, 11)
        }

        assertThrows(IllegalArgumentException::class.java) {
            YdbDecimalColumnType(10, -1)
        }
    }

    @Test
    fun `should parse decimal from db`() {
        val type = YdbDecimalColumnType(10, 2)

        assertEquals(BigDecimal("123.45"), type.valueFromDB(BigDecimal("123.45")))
        assertEquals(BigDecimal("123.45"), type.valueFromDB("123.45"))
    }

    @Test
    fun `should convert decimal to db with scale`() {
        val type = YdbDecimalColumnType(10, 2)

        assertEquals(BigDecimal("123.40"), type.notNullValueToDB(BigDecimal("123.4")))
        assertEquals("123.40", type.nonNullValueToString(BigDecimal("123.4")))
    }
}