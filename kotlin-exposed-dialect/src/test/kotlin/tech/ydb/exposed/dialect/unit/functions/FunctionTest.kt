package tech.ydb.exposed.dialect.unit.functions

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.functions.YdbFunctionProvider

object Users : Table("users") {

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
    fun `should generate correct CREATE TABLE`() {
        val ddlStatements = Users.ddl
        val ddl = ddlStatements.joinToString(" ")

        assertTrue(ddl.contains("CREATE TABLE users"))
        assertTrue(ddl.contains("id"))
        assertTrue(ddl.contains("name"))
        assertTrue(ddl.contains("PRIMARY KEY"))
    }

    @Test
    fun `should map integer to Int32`() {
        val column = Users.id
        val type = column.columnType.sqlType()

        assertTrue(type.contains("Int32") || type.contains("INT"))
    }
}