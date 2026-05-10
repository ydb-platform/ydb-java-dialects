package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.types.YdbJsonDocumentStringColumnType

class YdbJsonDocumentStringColumnTypeTest {

    private val type = YdbJsonDocumentStringColumnType()

    @Test
    fun `should return json document sql type`() {
        assertEquals("JsonDocument", type.sqlType())
    }

    @Test
    fun `should parse json document from db`() {
        val json = """{"name":"alice","active":true}"""
        assertEquals(json, type.valueFromDB(json))
    }

    @Test
    fun `should convert json document to db`() {
        val json = """{"name":"alice","active":true}"""
        assertEquals(json, type.notNullValueToDB(json))
        assertEquals("""'{"name":"alice","active":true}'""", type.nonNullValueToString(json))
    }

    @Test
    fun `should escape single quotes in json document`() {
        val json = """{"name":"O'Brien"}"""
        assertEquals("""'{"name":"O''Brien"}'""", type.nonNullValueToString(json))
    }
}
