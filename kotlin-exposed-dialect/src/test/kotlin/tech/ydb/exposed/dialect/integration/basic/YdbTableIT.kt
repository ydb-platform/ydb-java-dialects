package tech.ydb.exposed.dialect.integration.basic

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.YdbTtlColumnMode
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.ydbUint64
import tech.ydb.exposed.dialect.javatime.ydbTimestamp

class YdbTableIT : BaseYdbTest() {

    object BasicTable : YdbTable("unit_basic_table") {
        val id = integer("id")
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)
    }

    object TtlTimestampTable : YdbTable("unit_ttl_timestamp_table") {
        val id = integer("id")
        val expireAt = ydbTimestamp("expire_at")

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
    fun `renders CREATE TABLE with primary key`() = tx {
        val ddl = BasicTable.ddl.joinToString(" ")
        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS"), ddl)
        assertTrue(
            ddl.contains("PRIMARY KEY (id)") || ddl.contains("PRIMARY KEY (`id`)"),
            ddl
        )
        assertTrue(
            ddl.contains("`name` Text") || ddl.contains("name Text"),
            ddl
        )
    }

    @Test
    fun `renders TTL clause for a Timestamp64 column`() = tx {
        val ddl = TtlTimestampTable.ddl.joinToString(" ")
        assertTrue(ddl.contains("""WITH (TTL = Interval("PT1H") ON expire_at)"""), ddl)
        assertTrue(ddl.contains("Timestamp64"), ddl)
    }

    @Test
    fun `renders TTL clause for a numeric epoch column`() = tx {
        val ddl = TtlNumericTable.ddl.joinToString(" ")
        assertTrue(ddl.contains("""WITH (TTL = Interval("PT1H") ON modified_at_epoch AS SECONDS)"""), ddl)
        assertTrue(ddl.contains("modified_at_epoch Uint64"), ddl)
    }

    @Test
    fun `fails when the table has no primary key`() = tx {
        assertThrows(IllegalStateException::class.java) {
            NoPkTable.ddl
        }
    }

    @Test
    fun `fails when numeric TTL column type is unsupported`() = tx {
        assertThrows(IllegalArgumentException::class.java) {
            InvalidNumericTtlTable.ddl
        }
    }
}
