package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.types.YdbUuidAsStringColumnType
import java.util.UUID

class YdbUuidAsStringColumnTypeTest {

    private val type = YdbUuidAsStringColumnType()

    @Test
    fun `should return string uuid sql type`() {
        assertEquals("String", type.sqlType())
    }

    @Test
    fun `should parse uuid from db string and bytes`() {
        val uuid = UUID.randomUUID()

        assertEquals(uuid, type.valueFromDB(uuid))
        assertEquals(uuid, type.valueFromDB(uuid.toString()))
        assertEquals(uuid, type.valueFromDB(uuid.toString().toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `should convert uuid to db bytes`() {
        val uuid = UUID.randomUUID()

        val dbValue = type.notNullValueToDB(uuid)
        assertArrayEquals(uuid.toString().toByteArray(Charsets.UTF_8), dbValue as ByteArray)
        assertEquals("'$uuid'", type.nonNullValueToString(uuid))
    }
}