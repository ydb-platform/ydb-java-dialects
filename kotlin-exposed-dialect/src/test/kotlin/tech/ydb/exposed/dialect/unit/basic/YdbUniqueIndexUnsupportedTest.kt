package tech.ydb.exposed.dialect.unit.basic

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.*

class YdbUniqueIndexUnsupportedTest {

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
    fun `should reject unique standard index for current ydb runtime`() {
        transaction(db) {
            val dialect = db.dialect as YdbDialect

            assertThrows(IllegalArgumentException::class.java) {
                dialect.createIndex(T.emailIndexDefinition)
            }
        }
    }

    @Test
    fun `should reject unique ydb secondary index for current runtime`() {
        transaction(db) {
            assertThrows(IllegalArgumentException::class.java) {
                renderYdbSecondaryIndex(
                    YdbSecondaryIndexSpec(
                        name = "email_unique_idx",
                        columns = listOf(T.email),
                        unique = true
                    )
                )
            }
        }
    }
}