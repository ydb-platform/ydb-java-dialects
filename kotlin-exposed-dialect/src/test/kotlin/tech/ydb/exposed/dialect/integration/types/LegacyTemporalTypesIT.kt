package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.javatime.ydbDate
import tech.ydb.exposed.dialect.javatime.ydbDatetime
import tech.ydb.exposed.dialect.javatime.ydbTimestamp

class LegacyTemporalTypesIT : BaseYdbTest() {

    object LegacyTemporal : Table("legacy_temporal_types") {
        val id = integer("id")
        val dateCol = ydbDate("date_col")
        val dateTimeCol = ydbDatetime("datetime_col")
        val timestampCol = ydbTimestamp("timestamp_col")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = emptyList()

    @Test
    fun `unsigned ydb temporal extensions emit Date Datetime Timestamp`() = tx {
        SchemaUtils.create(LegacyTemporal)

        val ddl = LegacyTemporal.ddl.joinToString(" ")
        assertTrue(ddl.contains("date_col Date") && !ddl.contains("Date32"), ddl)
        assertTrue(ddl.contains("datetime_col Datetime") && !ddl.contains("Datetime64"), ddl)
        assertTrue(ddl.contains("timestamp_col Timestamp") && !ddl.contains("Timestamp64"), ddl)
    }
}
