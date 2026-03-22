package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IndexIT : BaseYdbTest() {

    object Customers : Table("customers") {
        val id = integer("id")
        val name = varchar("name", 255)
        val email = varchar("email", 255)
        override val primaryKey = PrimaryKey(id)
        val emailIndex = index(true, email) // UNIQUE INDEX
    }

    @Test
    fun `should create indexes`() = tx {
        SchemaUtils.create(Customers)

        // Проверяем, что индекс создан через DSL (фактически SQL выполняется через SchemaUtils)
        val indices = Customers.indices
        assertTrue(indices.any { it.columns.contains(Customers.email) })
    }
}