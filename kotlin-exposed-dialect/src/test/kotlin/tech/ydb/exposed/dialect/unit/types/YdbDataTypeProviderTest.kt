package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.types.YdbDataTypeProvider

class YdbDataTypeProviderTest {

    private val provider = YdbDataTypeProvider()

    @Test
    fun `should map integer types`() {
        Assertions.assertEquals("Int32", provider.integerType())
        Assertions.assertEquals("Int64", provider.longType())
        Assertions.assertEquals("Int16", provider.shortType())
    }

    @Test
    fun `should map string and binary types`() {
        Assertions.assertEquals("Utf8", provider.varcharType(255))
        Assertions.assertEquals("Utf8", provider.textType())
        Assertions.assertEquals("String", provider.binaryType())
        Assertions.assertEquals("String", provider.binaryType(100))
    }

    @Test
    fun `should map boolean type`() {
        Assertions.assertEquals("Bool", provider.booleanType())
    }

    @Test
    fun `should map UUID type`() {
        Assertions.assertEquals("Uuid", provider.uuidType())
    }

    @Test
    fun `should map JSON type`() {
        Assertions.assertEquals("Json", provider.jsonType())
    }

    @Test
    fun `should map numeric types`() {
        Assertions.assertEquals("Float", provider.floatType())
        Assertions.assertEquals("Double", provider.doubleType())
    }

    @Test
    fun `should map date and time types`() {
        Assertions.assertEquals("Date", provider.dateType())
        Assertions.assertEquals("Datetime", provider.dateTimeType())
        Assertions.assertEquals("Timestamp", provider.timestampType())
    }

    @Test
    fun `should map autoincrement type`() {
        Assertions.assertEquals("Int32", provider.integerAutoincType())
    }

    @Test
    fun `should convert hex to SQL`() {
        val hex = "0xABCD"
        Assertions.assertEquals("'0xABCD'", provider.hexToDb(hex))
    }
}