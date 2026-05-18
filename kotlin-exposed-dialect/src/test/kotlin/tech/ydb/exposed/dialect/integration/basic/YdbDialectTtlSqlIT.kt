package tech.ydb.exposed.dialect.integration.basic

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbDialect
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.YdbTtlColumnMode
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.ydbUint64
import tech.ydb.exposed.dialect.javatime.ydbTimestamp

class YdbDialectTtlSqlIT : BaseYdbTest() {

    object AlterTtlTimestampTable : YdbTable("unit_alter_ttl_timestamp_table") {
        val id = integer("id")
        val expireAt = ydbTimestamp("expire_at")

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
    fun `generates ALTER TABLE SET TTL for a timestamp column`() = tx {
        val dialect = db.dialect as YdbDialect
        val sql = dialect.setTtl(AlterTtlTimestampTable)

        assertTrue(sql.contains("ALTER TABLE"), sql)
        assertTrue(sql.contains("""SET (TTL = Interval("PT24H") ON expire_at)"""), sql)
    }

    @Test
    fun `generates ALTER TABLE SET TTL for a numeric epoch column`() = tx {
        val dialect = db.dialect as YdbDialect
        val sql = dialect.setTtl(AlterTtlNumericTable)

        assertTrue(sql.contains("ALTER TABLE"), sql)
        assertTrue(sql.contains("""SET (TTL = Interval("PT2H") ON modified_at_epoch AS SECONDS)"""), sql)
    }

    @Test
    fun `generates ALTER TABLE RESET TTL`() = tx {
        val dialect = db.dialect as YdbDialect
        val sql = dialect.resetTtl(AlterTtlTimestampTable)

        assertTrue(sql.contains("ALTER TABLE"), sql)
        assertTrue(sql.contains("RESET (TTL)"), sql)
    }

    @Test
    fun `rejects invalid TTL interval early`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            object : YdbTable("invalid_ttl_interval_table") {
                val id = integer("id")
                val expireAt = ydbTimestamp("expire_at")

                override val primaryKey = PrimaryKey(id)

                init {
                    ttl(expireAt, """PT1H" ON hacked""")
                }
            }
        }

        assertTrue(error.message?.contains("Invalid YDB TTL interval") == true, error.message)
    }
}
