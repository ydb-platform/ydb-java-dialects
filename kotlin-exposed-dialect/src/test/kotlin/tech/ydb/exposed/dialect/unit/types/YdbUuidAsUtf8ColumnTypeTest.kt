package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.types.YdbUuidAsUtf8ColumnType
import java.util.UUID

class YdbUuidAsUtf8ColumnTypeTest {

    private val type = YdbUuidAsUtf8ColumnType()

    @Test
    fun `should return utf8 uuid sql type`() {
        assertEquals("Utf8", type.sqlType())
    }

    @Test
    fun `should parse utf8 uuid from db`() {
        val uuid = UUID.randomUUID()

        assertEquals(uuid, type.valueFromDB(uuid))
        assertEquals(uuid, type.valueFromDB(uuid.toString()))
    }

    @Test
    fun `should convert utf8 uuid to db`() {
        val uuid = UUID.randomUUID()

        assertEquals(uuid.toString(), type.notNullValueToDB(uuid))
        assertEquals("'$uuid'", type.nonNullValueToString(uuid))
    }
}