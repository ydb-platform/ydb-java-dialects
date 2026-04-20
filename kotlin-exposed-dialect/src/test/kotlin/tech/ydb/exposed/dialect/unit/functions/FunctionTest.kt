package tech.ydb.exposed.dialect.unit.functions

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.functions.YdbFunctionProvider

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

class FunctionTests {
    private val provider = YdbFunctionProvider()

    @Test
    fun `should generate UPSERT statement`() {
        val data = listOf(
            Users.id to 1,
            Users.name to "Alice"
        )

        val sql = transaction {
            provider.upsert(
                table = Users,
                data = data,
                expression = "",
                onUpdate = emptyList(),
                keyColumns = emptyList(),
                where = null,
                transaction = this
            )
        }

        assertTrue(sql.startsWith("UPSERT"))
        assertTrue(sql.contains("users"))
        assertTrue(sql.contains("id"))
        assertTrue(sql.contains("name"))
    }

    @Test
    fun `should support NULL values`() {
        val data = listOf(
            Users.id to 1,
            Users.name to null
        )

        val sql = transaction {
            provider.upsert(
                table = Users,
                data = data,
                expression = "",
                onUpdate = emptyList(),
                keyColumns = emptyList(),
                where = null,
                transaction = this
            )
        }

        assertTrue(sql.contains("NULL"))
    }

    @Test
    fun `should reject WHERE in UPSERT`() {
        assertThrows(IllegalArgumentException::class.java) {
            transaction {
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
    fun `should ignore ON UPDATE in UPSERT`() {
        val sql = transaction {
            provider.upsert(
                table = Users,
                data = listOf(Users.id to 1, Users.name to "Alice"),
                expression = "",
                onUpdate = listOf(Users.name to "Bob"),
                keyColumns = emptyList(),
                where = null,
                transaction = this
            )
        }

        assertTrue(sql.startsWith("UPSERT"))
        assertTrue(!sql.contains("ON UPDATE"))
    }

    @Test
    fun `should add column list to prepared UPSERT values expression`() {
        val sql = transaction {
            provider.upsert(
                table = Users,
                data = listOf(Users.id to 1, Users.name to "Alice"),
                expression = "VALUES (?, ?)",
                onUpdate = listOf(Users.name to "Alice"),
                keyColumns = listOf(Users.id),
                where = null,
                transaction = this
            )
        }

        assertTrue(sql.contains("UPSERT INTO"))
        assertTrue(sql.contains("(id, name)") || sql.contains("(`id`, `name`)"))
        assertTrue(sql.contains("VALUES (?, ?)"))
    }

    @Test
    fun `should reject MERGE and point users to UPSERT`() {
        val error = assertThrows(UnsupportedOperationException::class.java) {
            transaction {
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

    @Test
    fun `should generate limit only`() {
        val sql = provider.queryLimitAndOffset(size = 10, offset = 0, alreadyOrdered = false)
        assertTrue(sql.contains("LIMIT 10"))
    }

    @Test
    fun `should generate limit and offset`() {
        val sql = provider.queryLimitAndOffset(size = 10, offset = 5, alreadyOrdered = false)
        assertTrue(sql.contains("LIMIT 10"))
        assertTrue(sql.contains("OFFSET 5"))
    }

    @Test
    fun `should generate offset without limit`() {
        val sql = provider.queryLimitAndOffset(size = null, offset = 5, alreadyOrdered = false)
        assertTrue(sql.contains("OFFSET 5"))
    }
}
