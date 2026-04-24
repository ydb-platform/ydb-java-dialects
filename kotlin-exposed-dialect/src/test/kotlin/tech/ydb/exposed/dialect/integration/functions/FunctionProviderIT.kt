package tech.ydb.exposed.dialect.integration.functions

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.functions.YdbFunctionProvider
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class FunctionProviderIT : BaseYdbTest() {

    object Users : Table("users") {
        val id = integer("id")
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)
    }

    object SourceUsers : Table("source_users") {
        val id = integer("id")
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)
    }

    private val provider = YdbFunctionProvider()

    @Test
    fun `should generate UPSERT statement`() = tx {
        val data = listOf(
            Users.id to 1,
            Users.name to "Alice"
        )

        val sql = provider.upsert(
            table = Users,
            data = data,
            expression = "",
            onUpdate = emptyList(),
            keyColumns = emptyList(),
            where = null,
            transaction = this
        )

        assertTrue(sql.startsWith("UPSERT"))
        assertTrue(sql.contains("users"))
        assertTrue(sql.contains("id"))
        assertTrue(sql.contains("name"))
    }

    @Test
    fun `should support NULL values`() = tx {
        val data = listOf(
            Users.id to 1,
            Users.name to null
        )

        val sql = provider.upsert(
            table = Users,
            data = data,
            expression = "",
            onUpdate = emptyList(),
            keyColumns = emptyList(),
            where = null,
            transaction = this
        )

        assertTrue(sql.contains("NULL"))
    }

    @Test
    fun `should reject WHERE in UPSERT`() {
        assertThrows(IllegalArgumentException::class.java) {
            tx {
                provider.upsert(
                    table = Users,
                    data = listOf(Users.id to 1, Users.name to "Alice"),
                    expression = "",
                    onUpdate = emptyList(),
                    keyColumns = emptyList(),
                    where = Users.id eq 1,
                    transaction = this
                )
            }
        }
    }

    @Test
    fun `should ignore ON UPDATE in UPSERT`() = tx {
        val sql = provider.upsert(
            table = Users,
            data = listOf(Users.id to 1, Users.name to "Alice"),
            expression = "",
            onUpdate = listOf(Users.name to "Bob"),
            keyColumns = emptyList(),
            where = null,
            transaction = this
        )

        assertTrue(sql.startsWith("UPSERT"))
        assertTrue(!sql.contains("ON UPDATE"))
    }

    @Test
    fun `should add column list to prepared UPSERT values expression`() = tx {
        val sql = provider.upsert(
            table = Users,
            data = listOf(Users.id to 1, Users.name to "Alice"),
            expression = "VALUES (?, ?)",
            onUpdate = listOf(Users.name to "Alice"),
            keyColumns = listOf(Users.id),
            where = null,
            transaction = this
        )

        assertTrue(sql.contains("UPSERT INTO"))
        assertTrue(
            sql.contains("(id, name)") ||
                    sql.contains("(`id`, `name`)") ||
                    sql.contains("(id, `name`)") ||
                    sql.contains("(`id`, name)"),
            sql
        )
        assertTrue(sql.contains("VALUES (?, ?)"))
    }

    @Test
    fun `should reject MERGE and point users to UPSERT`() {
        val error = assertThrows(UnsupportedOperationException::class.java) {
            tx {
                provider.merge(
                    dest = Users,
                    source = SourceUsers,
                    transaction = this,
                    clauses = emptyList(),
                    on = Users.id eq SourceUsers.id
                )
            }
        }

        assertEquals(
            "YDB dialect does not support ANSI MERGE through Exposed. Use UPSERT or batchUpsert instead.",
            error.message
        )
    }
}
