package tech.ydb.exposed.dialect.integration.dao

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

/**
 * YDB `Serial` works with Exposed `autoIncrement()` on a standard [Table].
 */
class SerialDaoIT : BaseYdbTest() {

    object Events : Table("serial_dao_events") {
        val id = integer("id").autoIncrement()
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Events)

    @Test
    fun `should create ddl with Serial and assign ids on insert`() = tx {
        val ddl = Events.ddl.joinToString(" ")
        assertTrue(ddl.contains("id Serial"), ddl)
        assertTrue(ddl.contains("PRIMARY KEY (id)") || ddl.contains("PRIMARY KEY (`id`)"), ddl)

        val firstId = Events.insert {
            it[name] = "opened"
        } get Events.id

        val secondId = Events.insert {
            it[name] = "closed"
        } get Events.id

        assertTrue(firstId > 0)
        assertTrue(secondId > firstId)

        val rows = Events.selectAll().orderBy(Events.id).toList()
        assertEquals(2, rows.size)
        assertEquals("opened", rows[0][Events.name])
        assertEquals("closed", rows[1][Events.name])
    }
}
