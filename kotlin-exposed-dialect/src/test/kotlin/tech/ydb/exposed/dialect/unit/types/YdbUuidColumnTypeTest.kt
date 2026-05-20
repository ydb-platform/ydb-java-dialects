package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbUuidColumnType
import java.util.UUID

class YdbUuidColumnTypeTest {

    private val type = YdbUuidColumnType()

    @Test
    fun `maps to native Uuid sql type`() {
        assertEquals("Uuid", type.sqlType())
    }

    @Test
    fun `valueFromDB accepts both UUID and String`() {
        val uuid = UUID.randomUUID()
        assertEquals(uuid, type.valueFromDB(uuid))
        assertEquals(uuid, type.valueFromDB(uuid.toString()))
    }

    @Test
    fun `notNullValueToDB returns the UUID itself (no string conversion)`() {
        val uuid = UUID.randomUUID()
        assertEquals(uuid, type.notNullValueToDB(uuid))
    }

    @Test
    fun `nonNullValueToString renders the YQL Uuid literal`() {
        val uuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
        assertEquals("Uuid(\"$uuid\")", type.nonNullValueToString(uuid))
    }
}
