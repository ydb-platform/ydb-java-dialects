package tech.ydb.exposed.dialect.integration.basic

import org.jetbrains.exposed.v1.core.Function
import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbDialect
import tech.ydb.exposed.dialect.createYdbStatement
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class YdbIndexSqlIT : BaseYdbTest() {

    object IndexedTable : Table("indexed_table") {
        val id = integer("id")
        val email = varchar("email", 255)
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)

        init {
            index(false, email)
            index("email-cover-idx", isUnique = true, email)
        }

        val emailIndexDefinition
            get() = indices.single { !it.unique && it.columns == listOf(email) }

        override fun createStatement() = createYdbStatement()
    }

    @Test
    fun `renders a standard Exposed index as YDB ALTER TABLE`() {
        transaction(db) {
            val dialect = db.dialect as YdbDialect
            val sql = dialect.createIndex(IndexedTable.emailIndexDefinition)

            assertTrue(sql.contains("ALTER TABLE"), sql)
            assertTrue(sql.contains("ADD INDEX"), sql)
            assertTrue(sql.contains("GLOBAL ON"), sql)
            assertTrue(sql.contains("email"), sql)
        }
    }

    @Test
    fun `rejects functional indexes`() {
        transaction(db) {
            val dialect = db.dialect as YdbDialect
            val functionIndex = Index(
                columns = emptyList(),
                unique = false,
                customName = "email_lower_idx",
                indexType = null,
                filterCondition = null,
                functions = listOf(
                    object : Function<String>(IndexedTable.email.columnType) {
                        override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                            queryBuilder.append("LOWER(email)")
                        }
                    }
                ),
                functionsTable = IndexedTable
            )

            val error = assertThrows(UnsupportedOperationException::class.java) {
                dialect.createIndex(functionIndex)
            }

            assertTrue(error.message == "YDB dialect does not support functional indexes", error.message)
        }
    }

    @Test
    fun `renders unique index with custom name`() {
        transaction(db) {
            val dialect = db.dialect as YdbDialect
            val uniqueIndex = IndexedTable.indices.single { it.unique }
            val sql = dialect.createIndex(uniqueIndex)
            val expectedName = db.identifierManager.cutIfNecessaryAndQuote("email-cover-idx")

            assertTrue(sql.contains("GLOBAL UNIQUE"), sql)
            assertTrue(sql.contains(expectedName), sql)
        }
    }
}
