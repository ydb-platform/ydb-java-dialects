package tech.ydb.exposed.dialect.integration.dao

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbUlidTable
import tech.ydb.exposed.dialect.basic.YdbUuidStringIdTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import java.util.UUID

class GeneratedIdsIT : BaseYdbTest() {

    object UuidItems : YdbUuidStringIdTable("generated_uuid_items") {
        val name = varchar("name", 255)
    }

    object UlidItems : YdbUlidTable("generated_ulid_items") {
        val name = varchar("name", 255)
    }

    override val tables: List<Table> = listOf(UuidItems, UlidItems)

    @Test
    fun `should generate uuid string id on insert`() = tx {
        UuidItems.insert {
            it[name] = "uuid-backed"
        }

        val row = UuidItems.selectAll().single()
        val id = row[UuidItems.id].value

        assertNotNull(UUID.fromString(id))
        assertEquals("uuid-backed", row[UuidItems.name])
    }

    @Test
    fun `should generate ulid id on insert`() = tx {
        UlidItems.insert {
            it[name] = "ulid-backed"
        }

        val row = UlidItems.selectAll().single()
        val id = row[UlidItems.id].value

        assertEquals(26, id.length)
        assertEquals("ulid-backed", row[UlidItems.name])
    }
}
