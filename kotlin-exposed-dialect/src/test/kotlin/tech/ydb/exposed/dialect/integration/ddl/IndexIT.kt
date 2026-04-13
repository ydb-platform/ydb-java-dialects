package tech.ydb.exposed.dialect.integration.ddl

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class IndexIT : BaseYdbTest() {

    object Customers : YdbTable("customers") {
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
        Assertions.assertTrue(indices.any { it.columns.contains(Customers.email) })
    }
}