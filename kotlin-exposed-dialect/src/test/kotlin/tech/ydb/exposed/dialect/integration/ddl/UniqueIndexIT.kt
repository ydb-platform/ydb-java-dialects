package tech.ydb.exposed.dialect.integration.ddl

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbIndexScope
import tech.ydb.exposed.dialect.basic.YdbIndexSyncMode
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class UniqueIndexIT : BaseYdbTest() {

    object UniqueCustomers : YdbTable("unique_customers") {
        val id = integer("id")
        val email = varchar("email", 255)
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)

        init {
            secondaryIndex(
                name = "unique_email_idx",
                email,
                unique = true,
                scope = YdbIndexScope.GLOBAL,
                syncMode = YdbIndexSyncMode.SYNC
            )
        }
    }

    override val tables: List<Table> = listOf(UniqueCustomers)

    @Test
    fun `should reject duplicate value for unique secondary index`() {
        tx {
            UniqueCustomers.insert {
                it[id] = 1
                it[email] = "alice@example.com"
                it[name] = "Alice"
            }
        }

        val error = assertThrows(ExposedSQLException::class.java) {
            tx {
                UniqueCustomers.insert {
                    it[id] = 2
                    it[email] = "alice@example.com"
                    it[name] = "Bob"
                }
            }
        }

        val message = error.message.orEmpty()
        assertTrue(
            message.contains("PRECONDITION_FAILED") ||
                    message.contains("duplicate", ignoreCase = true) ||
                    message.contains("unique", ignoreCase = true),
            message
        )

        tx {
            assertEquals(1, UniqueCustomers.selectAll().count())
        }
    }
}
