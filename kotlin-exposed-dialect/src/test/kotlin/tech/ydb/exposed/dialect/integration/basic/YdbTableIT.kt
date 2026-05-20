package tech.ydb.exposed.dialect.integration.basic

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.createYdbStatement
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.ydbUint64
import tech.ydb.exposed.dialect.javatime.ydbTimestamp64
import java.sql.SQLException

class YdbTableIT : BaseYdbTest() {

    object BasicTable : Table("unit_basic_table") {
        val id = integer("id")
        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)

        override fun createStatement() = createYdbStatement()
    }

    object TtlTimestampTable : Table("unit_ttl_timestamp_table") {
        val id = integer("id")
        val expireAt = ydbTimestamp64("expire_at")

        override val storageParameters: List<TableStorageParameter> =
            listOf(RawTableStorageParameter("TTL = Interval(\"PT1H\") ON expire_at"))

        override val primaryKey = PrimaryKey(id)

        override fun createStatement() = createYdbStatement()
    }

    object TtlNumericTable : Table("unit_ttl_numeric_table") {
        val id = integer("id")
        val modifiedAtEpoch = ydbUint64("modified_at_epoch")

        override val storageParameters: List<TableStorageParameter> =
            listOf(RawTableStorageParameter("TTL = Interval(\"PT1H\") ON modified_at_epoch AS SECONDS"))

        override val primaryKey = PrimaryKey(id)

        override fun createStatement() = createYdbStatement()
    }

    object NoPkTable : Table("unit_no_pk_table") {
        val id = integer("id")
        val name = varchar("name", 255)

        override fun createStatement() = createYdbStatement()
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
}
