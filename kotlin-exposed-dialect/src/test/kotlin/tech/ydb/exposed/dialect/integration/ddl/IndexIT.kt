package tech.ydb.exposed.dialect.integration.ddl

import org.jetbrains.exposed.v1.core.Table
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbDialect
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class IndexIT : BaseYdbTest() {

    object Customers : YdbTable("customers") {
        val id = integer("id")
        val name = varchar("name", 255)
        val email = varchar("email", 255)

        override val primaryKey = PrimaryKey(id)

        init {
            index(false, email)
        }

        val emailIndexDefinition
            get() = indices.single { it.columns == listOf(email) }
    }

    override val tables: List<Table> = listOf(Customers)

    @Test
    fun `should generate standard exposed index sql`() = tx {
        val dialect = db.dialect as YdbDialect
        val sql = dialect.createIndex(Customers.emailIndexDefinition)

        assertTrue(sql.contains("ALTER TABLE"), sql)
        assertTrue(sql.contains("ADD INDEX"), sql)
        assertTrue(sql.contains("GLOBAL ON"), sql)
        assertTrue(sql.contains("email"), sql)
    }

    @Test
    fun `should read existing indexes from jdbc metadata`() = tx {
        val indexes = db.dialectMetadata.existingIndices(Customers).getValue(Customers)
        val byName = indexes.associateBy { it.indexName }

        assertTrue("customers_email" in byName.keys, indexes.joinToString { it.indexName })
        assertEquals(listOf(Customers.email), byName.getValue("customers_email").columns)
    }
}
