package tech.ydb.exposed.dialect.unit.basic

import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbDialect
import tech.ydb.exposed.dialect.basic.YdbDialectProvider
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.basic.YdbTtlColumnMode
import tech.ydb.exposed.dialect.types.ydbUint64

class YdbDialectTtlSqlTest {

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

    object AlterTtlTimestampTable : YdbTable("unit_alter_ttl_timestamp_table") {
        val id = integer("id")
        val expireAt = timestamp("expire_at")

        override val primaryKey = PrimaryKey(id)

        init {
            ttl(expireAt, "PT24H")
        }
    }

    object AlterTtlNumericTable : YdbTable("unit_alter_ttl_numeric_table") {
        val id = integer("id")
        val modifiedAtEpoch = ydbUint64("modified_at_epoch")

        override val primaryKey = PrimaryKey(id)

        init {
            ttl(modifiedAtEpoch, "PT2H", YdbTtlColumnMode.SECONDS)
        }
    }

    @Test
    fun `should generate alter table set ttl for timestamp`() = transaction(db) {
        val dialect = db.dialect as YdbDialect
        val sql = dialect.setTtl(AlterTtlTimestampTable)

        assertTrue(sql.contains("ALTER TABLE"))
        assertTrue(sql.contains("""SET (TTL = Interval("PT24H") ON expire_at)"""))
    }

    @Test
    fun `should generate alter table set ttl for numeric column`() = transaction(db) {
        val dialect = db.dialect as YdbDialect
        val sql = dialect.setTtl(AlterTtlNumericTable)

        assertTrue(sql.contains("ALTER TABLE"))
        assertTrue(sql.contains("""SET (TTL = Interval("PT2H") ON modified_at_epoch AS SECONDS)"""))
    }

    @Test
    fun `should generate alter table reset ttl`() = transaction(db) {
        val dialect = db.dialect as YdbDialect
        val sql = dialect.resetTtl(AlterTtlTimestampTable)

        assertTrue(sql.contains("ALTER TABLE"))
        assertTrue(sql.contains("RESET (TTL)"))
    }
}