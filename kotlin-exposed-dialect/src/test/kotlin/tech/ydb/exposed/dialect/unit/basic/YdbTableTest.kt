package tech.ydb.exposed.dialect.unit.basic

import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbDialectProvider
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.basic.YdbTtlColumnMode
import tech.ydb.exposed.dialect.types.ydbUint64

class YdbTableTest {

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

    object BasicTable : YdbTable("unit_basic_table") {
        val id = integer("id")
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)
    }

    object TtlTimestampTable : YdbTable("unit_ttl_timestamp_table") {
        val id = integer("id")
        val expireAt = timestamp("expire_at")

        override val primaryKey = PrimaryKey(id)

        init {
            ttl(expireAt, "PT1H")
        }
    }

    object TtlNumericTable : YdbTable("unit_ttl_numeric_table") {
        val id = integer("id")
        val modifiedAtEpoch = ydbUint64("modified_at_epoch")

        override val primaryKey = PrimaryKey(id)

        init {
            ttl(modifiedAtEpoch, "PT1H", YdbTtlColumnMode.SECONDS)
        }
    }

    object NoPkTable : YdbTable("unit_no_pk_table") {
        val id = integer("id")
        val name = varchar("name", 255)
    }

    object InvalidNumericTtlTable : YdbTable("unit_invalid_numeric_ttl_table") {
        val id = integer("id")
        val modifiedAtEpoch = integer("modified_at_epoch")

        override val primaryKey = PrimaryKey(id)

        init {
            ttl(modifiedAtEpoch, "PT1H", YdbTtlColumnMode.SECONDS)
        }
    }

    @Test
    fun `should generate create table with primary key clause`() {
        transaction(db) {
            val ddl = BasicTable.ddl.joinToString(" ")

            assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS"), ddl)
            assertTrue(
                ddl.contains("PRIMARY KEY (id)") || ddl.contains("PRIMARY KEY (`id`)"),
                ddl
            )
            assertTrue(
                ddl.contains("`name` Utf8") || ddl.contains("name Utf8"),
                ddl
            )
        }
    }

    @Test
    fun `should generate ttl clause for timestamp column`() = transaction(db) {
        val ddl = TtlTimestampTable.ddl.joinToString(" ")

        assertTrue(ddl.contains("""WITH (TTL = Interval("PT1H") ON expire_at)"""))
    }

    @Test
    fun `should generate ttl clause for numeric epoch column`() = transaction(db) {
        val ddl = TtlNumericTable.ddl.joinToString(" ")

        assertTrue(ddl.contains("""WITH (TTL = Interval("PT1H") ON modified_at_epoch AS SECONDS)"""))
        assertTrue(ddl.contains("modified_at_epoch Uint64"))
    }

    @Test
    fun `should fail when table has no primary key`() {
        transaction(db) {
            assertThrows(IllegalStateException::class.java) {
                NoPkTable.ddl
            }
        }
    }

    @Test
    fun `should fail for unsupported numeric ttl column type`() {
        transaction(db) {
            assertThrows(IllegalArgumentException::class.java) {
                InvalidNumericTtlTable.ddl
            }
        }
    }
}