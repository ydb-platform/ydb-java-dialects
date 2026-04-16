package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.types.ydbJson

class JsonTypesIT : BaseYdbTest() {

    object JsonTypes : YdbTable("json_types") {
        val id = integer("id")
        val payload = ydbJson("payload")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(JsonTypes)

    @Test
    fun `should round-trip json type`() = tx {
        val json = """{"name":"alice","active":true}"""

        JsonTypes.insert {
            it[id] = 1
            it[payload] = json
        }

        val row = JsonTypes.selectAll().single()
        assertEquals(json, row[JsonTypes.payload])
    }

    @Test
    fun `should generate ddl for json type`() = tx {
        val ddl = JsonTypes.ddl.joinToString(" ")
        assertTrue(ddl.contains("payload Json"))
    }
}