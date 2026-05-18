package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbDataTypeProvider

class YdbDataTypeProviderTest {

    private val provider = YdbDataTypeProvider(enableSignedDatetimes = false)

    @Test
    fun `maps integer types`() {
        assertEquals("Int32", provider.integerType())
        assertEquals("Int64", provider.longType())
        assertEquals("Uint64", provider.ulongType())
        assertEquals("Int16", provider.shortType())
        assertEquals("Uint8", provider.ubyteType())
        assertEquals("Uint16", provider.ushortType())
        assertEquals("Uint32", provider.uintegerType())
    }

    @Test
    fun `maps string and binary types`() {
        assertEquals("Text", provider.varcharType(255))
        assertEquals("Text", provider.textType())
        assertEquals("Bytes", provider.binaryType())
        assertEquals("Bytes", provider.binaryType(100))
    }

    @Test
    fun `maps boolean and UUID and JSON types`() {
        assertEquals("Bool", provider.booleanType())
        assertEquals("Uuid", provider.uuidType())
        assertEquals("Json", provider.jsonType())
    }

    @Test
    fun `maps floating-point types`() {
        assertEquals("Float", provider.floatType())
        assertEquals("Double", provider.doubleType())
    }

    @Test
    fun `maps standard temporal types to legacy Date Datetime Timestamp`() {
        assertEquals("Date", provider.dateType())
        assertEquals("Datetime", provider.dateTimeType())
        assertEquals("Timestamp", provider.timestampType())
    }

    @Test
    fun `maps temporal types to Date32 Datetime64 Timestamp64 when enableSignedDatetimes`() {
        val signed = YdbDataTypeProvider(enableSignedDatetimes = true)
        assertEquals("Date32", signed.dateType())
        assertEquals("Datetime64", signed.dateTimeType())
        assertEquals("Timestamp64", signed.timestampType())
    }

    @Test
    fun `maps autoincrement to Serial and BigSerial`() {
        assertEquals("Serial", provider.integerAutoincType())
        assertEquals("BigSerial", provider.longAutoincType())
    }

    @Test
    fun `rejects unsigned autoincrement`() {
        assertThrows(UnsupportedOperationException::class.java) {
            provider.uintegerAutoincType()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            provider.ulongAutoincType()
        }
    }

}
