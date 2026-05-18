package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.code.YdbJdbcCode
import tech.ydb.exposed.dialect.YdbJsonStringColumnType

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

    @Test
    fun `should bind json with explicit JDBC vendor type`() {
        val json = """{"name":"alice","active":true}"""
        val (stmt, capture) = ydbPreparedStatementCapture()

        type.setParameter(stmt, 1, json)

        val actual = capture()
        assertNotNull(actual)
        assertEquals(BoundSqlObject(1, json, YdbJdbcCode.JSON), actual)
    }
}
