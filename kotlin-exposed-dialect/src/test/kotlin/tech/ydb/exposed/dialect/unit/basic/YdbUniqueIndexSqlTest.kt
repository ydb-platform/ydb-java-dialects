package tech.ydb.exposed.dialect.unit.basic

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbDialect
import tech.ydb.exposed.dialect.basic.YdbDialectProvider
import tech.ydb.exposed.dialect.basic.YdbSecondaryIndexSpec
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.basic.renderYdbSecondaryIndex

class YdbUniqueIndexSqlTest {

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

    object T : YdbTable("t_unique_idx_test") {
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
    fun `should render unique standard exposed index`() {
        transaction(db) {
            val dialect = db.dialect as YdbDialect

            val sql = dialect.createIndex(T.emailIndexDefinition)

            assertTrue(sql.contains("ADD INDEX"), sql)
            assertTrue(sql.contains("GLOBAL UNIQUE"), sql)
            assertTrue(sql.contains("ON (`email`)") || sql.contains("ON (email)"), sql)
        }
    }

    @Test
    fun `should render unique ydb secondary index`() {
        transaction(db) {
            val sql = renderYdbSecondaryIndex(
                YdbSecondaryIndexSpec(
                    name = "email_unique_idx",
                    columns = listOf(T.email),
                    unique = true
                )
            )

            assertTrue(sql.contains("INDEX email_unique_idx GLOBAL UNIQUE"), sql)
            assertTrue(sql.contains("ON (`email`)") || sql.contains("ON (email)"), sql)
        }
    }
}
