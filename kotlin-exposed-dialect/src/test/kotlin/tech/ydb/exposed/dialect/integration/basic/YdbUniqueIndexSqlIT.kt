package tech.ydb.exposed.dialect.integration.basic

import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.Table
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbDialect
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class YdbUniqueIndexSqlIT : BaseYdbTest() {

    object T : Table("t_unique_idx_test") {
        val id = integer("id")
        val email = varchar("email", 255)

        override val primaryKey = PrimaryKey(id)

        init {
            index(true, email)
        }

        val emailIndexDefinition
            get() = indices.single { it.columns == listOf(email) }
    }

    @Test
    fun `renders a unique standard Exposed index`() = tx {
        val dialect = db.dialect as YdbDialect

        val sql = dialect.createIndex(T.emailIndexDefinition)

        assertTrue(sql.contains("ADD INDEX"), sql)
        assertTrue(sql.contains("GLOBAL UNIQUE"), sql)
        assertTrue(sql.contains("ON (`email`)") || sql.contains("ON (email)"), sql)
    }

    @Test
    fun `quotes a custom index name through the identifier manager`() = tx {
        val dialect = db.dialect as YdbDialect
        val index = Index(
            columns = listOf(T.email),
            unique = false,
            customName = "select",
            indexType = null,
            filterCondition = null,
            functions = emptyList(),
            functionsTable = T
        )

        val sql = dialect.createIndex(index)

        assertTrue(sql.contains("ADD INDEX"), sql)
        assertTrue(sql.contains("`select`") || sql.contains("\"select\""), sql)
    }
}
