package tech.ydb.exposed.dialect.integration.basic

import org.jetbrains.exposed.v1.core.Function
import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbDialect
import tech.ydb.exposed.dialect.YdbIndexScope
import tech.ydb.exposed.dialect.YdbIndexSyncMode
import tech.ydb.exposed.dialect.YdbSecondaryIndexSpec
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

class YdbIndexSqlIT : BaseYdbTest() {

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
    fun `renders a standard Exposed index as YDB ALTER TABLE`() = tx {
        val dialect = db.dialect as YdbDialect
        val sql = dialect.createIndex(IndexedTable.emailIndexDefinition)

        assertTrue(sql.contains("ALTER TABLE"), sql)
        assertTrue(sql.contains("ADD INDEX"), sql)
        assertTrue(sql.contains("GLOBAL ON"), sql)
        assertTrue(sql.contains("email"), sql)
    }

    @Test
    fun `rejects functional indexes`() = tx {
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

    @Test
    fun `renders YDB-specific inline secondary index`() = tx {
        val ddl = IndexedTable.ddl.joinToString(" ")

        assertTrue(ddl.contains("INDEX email_cover_idx"), ddl)
        assertTrue(ddl.contains("GLOBAL ASYNC"), ddl)
        assertTrue(ddl.contains("ON (`email`)") || ddl.contains("ON (email)"), ddl)
        assertTrue(ddl.contains("COVER (`name`)") || ddl.contains("COVER (name)"), ddl)
        assertTrue(ddl.contains("WITH (foo = \"bar\")"), ddl)
    }

    @Test
    fun `renders YDB-specific ALTER TABLE secondary index SQL`() {
        val dialect = db.dialect as YdbDialect

        val sql = dialect.createSecondaryIndex(
            table = IndexedTable,
            spec = YdbSecondaryIndexSpec(
                name = "email_lookup_idx",
                columns = listOf(IndexedTable.email),
                unique = false,
                scope = YdbIndexScope.GLOBAL,
                syncMode = YdbIndexSyncMode.SYNC
            ),
            database = db
        )

        assertTrue(sql.contains("ALTER TABLE"), sql)
        assertTrue(sql.contains("ADD INDEX email_lookup_idx GLOBAL"), sql)
        assertTrue(sql.contains("ON (`email`)") || sql.contains("ON (email)"), sql)
    }

    @Test
    fun `renders UNIQUE YDB-specific ALTER TABLE secondary index SQL`() {
        val dialect = db.dialect as YdbDialect

        val sql = dialect.createSecondaryIndex(
            table = IndexedTable,
            spec = YdbSecondaryIndexSpec(
                name = "email_unique_lookup_idx",
                columns = listOf(IndexedTable.email),
                unique = true,
                scope = YdbIndexScope.GLOBAL,
                syncMode = YdbIndexSyncMode.SYNC
            ),
            database = db
        )

        assertTrue(sql.contains("ALTER TABLE"), sql)
        assertTrue(sql.contains("ADD INDEX email_unique_lookup_idx GLOBAL UNIQUE"), sql)
        assertTrue(sql.contains("ON (`email`)") || sql.contains("ON (email)"), sql)
    }

    @Test
    fun `quotes secondary index name when needed`() {
        val dialect = db.dialect as YdbDialect
        val expectedName = db.identifierManager.cutIfNecessaryAndQuote("email-cover-idx")

        val sql = dialect.createSecondaryIndex(
            table = IndexedTable,
            spec = YdbSecondaryIndexSpec(
                name = "email-cover-idx",
                columns = listOf(IndexedTable.email),
                unique = false,
                scope = YdbIndexScope.GLOBAL,
                syncMode = YdbIndexSyncMode.SYNC
            ),
            database = db
        )

        assertTrue(sql.contains("ADD INDEX $expectedName GLOBAL"), sql)
    }
}
