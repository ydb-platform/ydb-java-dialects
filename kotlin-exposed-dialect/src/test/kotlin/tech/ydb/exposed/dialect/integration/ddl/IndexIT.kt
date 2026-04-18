package tech.ydb.exposed.dialect.integration.ddl

import org.jetbrains.exposed.v1.core.Table
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbDialect
import tech.ydb.exposed.dialect.basic.YdbIndexScope
import tech.ydb.exposed.dialect.basic.YdbIndexSyncMode
import tech.ydb.exposed.dialect.basic.YdbSecondaryIndexSpec
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class IndexIT : BaseYdbTest() {

    object Customers : YdbTable("customers") {
        val id = integer("id")
        val name = varchar("name", 255)
        val email = varchar("email", 255)

        override val primaryKey = PrimaryKey(id)

        init {
            index(false, email)

            secondaryIndex(
                name = "email_name_cover_idx",
                email,
                unique = false,
                scope = YdbIndexScope.GLOBAL,
                syncMode = YdbIndexSyncMode.ASYNC,
                coverColumns = listOf(name)
            )
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
    fun `should generate inline ydb secondary index in create table ddl`() = tx {
        val ddl = Customers.ddl.joinToString(" ")

        assertTrue(ddl.contains("INDEX email_name_cover_idx"), ddl)
        assertTrue(ddl.contains("GLOBAL ASYNC"), ddl)
        assertTrue(ddl.contains("ON (`email`)") || ddl.contains("ON (email)"), ddl)
        assertTrue(ddl.contains("COVER (`name`)") || ddl.contains("COVER (name)"), ddl)
        assertTrue(ddl.contains("PRIMARY KEY"), ddl)
    }

    @Test
    fun `should generate alter table sql for ydb specific secondary index`() = tx {
        val dialect = db.dialect as YdbDialect

        val spec = YdbSecondaryIndexSpec(
            name = "email_lookup_idx",
            columns = listOf(Customers.email),
            unique = false,
            scope = YdbIndexScope.GLOBAL,
            syncMode = YdbIndexSyncMode.SYNC
        )

        val sql = dialect.createSecondaryIndex(Customers, spec)

        assertTrue(sql.contains("ALTER TABLE"), sql)
        assertTrue(sql.contains("ADD INDEX email_lookup_idx GLOBAL"), sql)
        assertTrue(sql.contains("ON (`email`)") || sql.contains("ON (email)"), sql)
    }
}