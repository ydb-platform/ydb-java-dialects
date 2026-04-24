package tech.ydb.exposed.dialect.integration.crud

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class CrudIT : BaseYdbTest() {

    object Users : YdbTable("users") {
        val id = integer("id")
        val name = varchar("name", 255)
        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(Users)

    @Test
    fun `should perform full CRUD`() = tx {
        Users.insert { it[id] = 1; it[name] = "Alice" }

        // READ
        val user = Users.selectAll().single()
        Assertions.assertEquals("Alice", user[Users.name])

        // UPDATE
        Users.update({ Users.id eq 1 }) { it[name] = "Bob" }
        val updated = Users.selectAll().single()
        Assertions.assertEquals("Bob", updated[Users.name])

        // DELETE
        Users.deleteWhere { Users.id eq 1 }
        val count = Users.selectAll().count()
        Assertions.assertEquals(0, count)
    }
}