package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbDecimalColumnType
import tech.ydb.exposed.dialect.code.YdbJdbcCode
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

    @Test
    fun `should bind decimal with JDBC vendor code for precision and scale`() {
        val type = YdbDecimalColumnType(10, 2)
        val value = BigDecimal("123.40")
        val (stmt, capture) = ydbPreparedStatementCapture()

        type.setParameter(stmt, 1, value)

        val actual = capture()
        org.junit.jupiter.api.Assertions.assertNotNull(actual)
        assertEquals(BoundSqlObject(1, BigDecimal("123.40"), YdbJdbcCode.decimal(10, 2)), actual)
    }

    @Test
    fun `should reject decimal with scale greater than column scale`() {
        val type = YdbDecimalColumnType(10, 2)

        val error1 = assertThrows(IllegalArgumentException::class.java) {
            type.notNullValueToDB(BigDecimal("123.456"))
        }
        assertEquals(
            "YDB Decimal value 123.456 has scale 3, which exceeds column scale 2",
            error1.message
        )

        val error2 = assertThrows(IllegalArgumentException::class.java) {
            type.nonNullValueToString(BigDecimal("123.456"))
        }
        assertEquals(
            "YDB Decimal value 123.456 has scale 3, which exceeds column scale 2",
            error2.message
        )
    }
}
