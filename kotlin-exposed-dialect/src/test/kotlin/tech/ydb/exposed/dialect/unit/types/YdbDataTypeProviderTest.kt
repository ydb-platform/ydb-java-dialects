package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbDataTypeProvider

class YdbDataTypeProviderTest {

    private val provider = YdbDataTypeProvider(forceLegacyDatetimes = false)
    private val legacyProvider = YdbDataTypeProvider(forceLegacyDatetimes = true)

    @Test
    fun `maps integer types`() {
        assertEquals("Int32", provider.integerType())
        assertEquals("Int64", provider.longType())
        assertEquals("Int16", provider.shortType())
    }

    @Test
    fun `maps string and binary types`() {
        assertEquals("Text", provider.varcharType(255))
        assertEquals("Text", provider.textType())
        assertEquals("String", provider.binaryType())
        assertEquals("String", provider.binaryType(100))
    }

    @Test
    fun `maps boolean and UUID and JSON types`() {
        assertEquals("Bool", provider.booleanType())
        assertEquals("Uuid", provider.uuidType())
        assertEquals("JsonDocument", provider.jsonType())
    }

    @Test
    fun `maps floating-point types`() {
        assertEquals("Float", provider.floatType())
        assertEquals("Double", provider.doubleType())
    }

    @Test
    fun `defaults temporal types to extended (Date32 Datetime64 Timestamp64)`() {
        assertEquals("Date32", provider.dateType())
        assertEquals("Datetime64", provider.dateTimeType())
        assertEquals("Timestamp64", provider.timestampType())
    }

    @Test
    fun `forceLegacyDatetimes switches to Date Datetime Timestamp`() {
        assertEquals("Date", legacyProvider.dateType())
        assertEquals("Datetime", legacyProvider.dateTimeType())
        assertEquals("Timestamp", legacyProvider.timestampType())
    }

    @Test
    fun `rejects autoincrement type`() {
        assertThrows(UnsupportedOperationException::class.java) {
            provider.integerAutoincType()
        }
    }

    @Test
    fun `hexToDb wraps in single quotes`() {
        assertEquals("'0xABCD'", provider.hexToDb("0xABCD"))
    }
}
