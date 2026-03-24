package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TypesIT {

    object TestTable : Table("test_types") {
        val id = integer("id")
        val text = varchar("text", 255)

        override val primaryKey = PrimaryKey(id)
    }

    @Test
    fun `should map types correctly`() {

        transaction {

            val ddl = TestTable.ddl.joinToString(" ")

            Assertions.assertTrue(ddl.contains("Int32"))
            Assertions.assertTrue(ddl.contains("Utf8") || ddl.contains("String"))
        }
    }
}