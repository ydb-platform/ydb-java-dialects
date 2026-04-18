package tech.ydb.exposed.dialect.unit.basic

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.*

class YdbIndexSqlTest {

    companion object {
        private lateinit var db: Database

        @JvmStatic
        @BeforeAll
        fun setupDb() {
            db = YdbDialectProvider.connect(
                url = "jdbc:ydb:grpc://localhost:2136/local",
                driver = "tech.ydb.jdbc.YdbDriver"
            )
        }
    }

    object IndexedTable : YdbTable("indexed_table") {
        val id = integer("id")
        val email = varchar("email", 255)
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)

        init {
            index(false, email)

            secondaryIndex(
                name = "email_cover_idx",
                email,
                unique = false,
                scope = YdbIndexScope.GLOBAL,
                syncMode = YdbIndexSyncMode.ASYNC,
                coverColumns = listOf(name),
                withParams = mapOf("foo" to "bar")
            )
        }

        val emailIndexDefinition
            get() = indices.single { it.columns == listOf(email) }
    }

    @Test
    fun `should render standard exposed index as ydb alter table sql`() {
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
    fun `should render ydb specific inline secondary index`() {
        transaction(db) {
            val ddl = IndexedTable.ddl.joinToString(" ")

            assertTrue(ddl.contains("INDEX email_cover_idx"), ddl)
            assertTrue(ddl.contains("GLOBAL ASYNC"), ddl)
            assertTrue(ddl.contains("ON (`email`)") || ddl.contains("ON (email)"), ddl)
            assertTrue(ddl.contains("COVER (`name`)") || ddl.contains("COVER (name)"), ddl)
            assertTrue(ddl.contains("WITH (foo = \"bar\")"), ddl)
        }
    }

    @Test
    fun `should render ydb specific alter table secondary index sql`() {
        transaction(db) {
            val dialect = db.dialect as YdbDialect

            val sql = dialect.createSecondaryIndex(
                table = IndexedTable,
                spec = YdbSecondaryIndexSpec(
                    name = "email_lookup_idx",
                    columns = listOf(IndexedTable.email),
                    unique = false,
                    scope = YdbIndexScope.GLOBAL,
                    syncMode = YdbIndexSyncMode.SYNC
                )
            )

            assertTrue(sql.contains("ALTER TABLE"), sql)
            assertTrue(sql.contains("ADD INDEX email_lookup_idx GLOBAL"), sql)
            assertTrue(sql.contains("ON (`email`)") || sql.contains("ON (email)"), sql)
        }
    }
}