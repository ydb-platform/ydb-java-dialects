package tech.ydb.exposed.dialect.integration.locking

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.locking.YdbOptimisticLocking

class OptimisticLockingIT : BaseYdbTest() {

    object Documents : YdbTable("documents") {
        val id = integer("id")
        val title = varchar("title", 255)
        val version = integer("version")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Documents)

    @Test
    fun `should update row when expected version matches`() = tx {
        Documents.insert {
            it[id] = 1
            it[title] = "draft"
            it[version] = 0
        }

        val updated = YdbOptimisticLocking.updateWithVersion(
            table = Documents,
            idColumn = Documents.id,
            idValue = 1,
            versionColumn = Documents.version,
            expectedVersion = 0
        ) {
            it[Documents.title] = "published"
        }

        assertTrue(updated)

        val row = Documents.selectAll().single()
        assertEquals("published", row[Documents.title])
        assertEquals(1, row[Documents.version])
    }

    @Test
    fun `should not update row when expected version does not match`() = tx {
        Documents.insert {
            it[id] = 1
            it[title] = "draft"
            it[version] = 1
        }

        val updated = YdbOptimisticLocking.updateWithVersion(
            table = Documents,
            idColumn = Documents.id,
            idValue = 1,
            versionColumn = Documents.version,
            expectedVersion = 0
        ) {
            it[Documents.title] = "published"
        }

        assertFalse(updated)

        val row = Documents.selectAll().single()
        assertEquals("draft", row[Documents.title])
        assertEquals(1, row[Documents.version])
    }
}