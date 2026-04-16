package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.types.YdbUuidColumnType
import java.util.UUID

class YdbUuidColumnTypeTest {

    private val type = YdbUuidColumnType()

    @Test
    fun `should return native uuid sql type`() {
        assertEquals("Uuid", type.sqlType())
    }

    @Test
    fun `should parse native uuid from db`() {
        val uuid = UUID.randomUUID()

        assertEquals(uuid, type.valueFromDB(uuid))
        assertEquals(uuid, type.valueFromDB(uuid.toString()))
    }

    @Test
    fun `should convert native uuid to db`() {
        val uuid = UUID.randomUUID()

        assertEquals(uuid.toString(), type.notNullValueToDB(uuid))
        assertEquals("'$uuid'", type.nonNullValueToString(uuid))
    }
}