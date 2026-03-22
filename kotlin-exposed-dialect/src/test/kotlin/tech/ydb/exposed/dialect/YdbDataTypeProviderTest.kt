package tech.ydb.exposed.dialect

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YdbDataTypeProviderTest {

    private val provider = YdbDataTypeProvider()

    @Test
    fun `should map integer types`() {
        assertEquals("Int32", provider.integerType())
        assertEquals("Int64", provider.longType())
        assertEquals("Int16", provider.shortType())
    }

    @Test
    fun `should map string types`() {
        assertEquals("Utf8", provider.varcharType(255))
        assertEquals("String", provider.textType())
        assertEquals("String", provider.binaryType())
        assertEquals("String", provider.binaryType(100))
    }

    @Test
    fun `should map boolean type`() {
        assertEquals("Bool", provider.booleanType())
    }

    @Test
    fun `should map UUID type`() {
        assertEquals("Uuid", provider.uuidType())
    }

    @Test
    fun `should map JSON type`() {
        assertEquals("Json", provider.jsonType())
    }

    @Test
    fun `should map numeric types`() {
        assertEquals("Float", provider.floatType())
        assertEquals("Double", provider.doubleType())
    }

    @Test
    fun `should map date and time types`() {
        assertEquals("Date", provider.dateType())
        assertEquals("Datetime", provider.dateTimeType())
    }

    @Test
    fun `should map autoincrement type`() {
        assertEquals("Int32", provider.integerAutoincType())
    }

    @Test
    fun `should convert hex to SQL`() {
        val hex = "0xABCD"
        assertEquals("'0xABCD'", provider.hexToDb(hex))
    }
}