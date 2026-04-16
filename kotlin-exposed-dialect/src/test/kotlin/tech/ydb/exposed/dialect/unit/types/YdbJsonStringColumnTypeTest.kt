package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.types.YdbJsonStringColumnType

class YdbJsonStringColumnTypeTest {

    private val type = YdbJsonStringColumnType()

    @Test
    fun `should return json sql type`() {
        assertEquals("Json", type.sqlType())
    }

    @Test
    fun `should parse json from db`() {
        val json = """{"name":"alice","active":true}"""
        assertEquals(json, type.valueFromDB(json))
    }

    @Test
    fun `should convert json to db`() {
        val json = """{"name":"alice","active":true}"""
        assertEquals(json, type.notNullValueToDB(json))
        assertEquals("""'{"name":"alice","active":true}'""", type.nonNullValueToString(json))
    }

    @Test
    fun `should escape single quotes in json`() {
        val json = """{"name":"O'Brien"}"""
        assertEquals("""'{"name":"O''Brien"}'""", type.nonNullValueToString(json))
    }
}