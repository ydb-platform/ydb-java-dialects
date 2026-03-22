package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertTrue
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

            assertTrue(ddl.contains("Int32"))
            assertTrue(ddl.contains("Utf8") || ddl.contains("String"))
        }
    }
}