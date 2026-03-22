package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CrudIT : BaseYdbTest() {

    object Users : Table("users") {
        val id = integer("id")
        val name = varchar("name", 255)
        override val primaryKey = PrimaryKey(id)
    }

    @Test
    fun `should perform full CRUD`() = tx {
        SchemaUtils.create(Users)

        // CREATE
        Users.insert { it[id] = 1; it[name] = "Alice" }

        // READ
        val user = Users.selectAll().single()
        assertEquals("Alice", user[Users.name])

        // UPDATE
        Users.update({ Users.id eq 1 }) { it[name] = "Bob" }
        val updated = Users.selectAll().single()
        assertEquals("Bob", updated[Users.name])

        // DELETE
        Users.deleteWhere { Users.id eq 1 }
        val count = Users.selectAll().count()
        assertEquals(0, count)
    }
}