package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.javatime.ydbDate
import tech.ydb.exposed.dialect.javatime.ydbDatetime
import tech.ydb.exposed.dialect.javatime.ydbTimestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/** [ydbDate] / [ydbDatetime] / [ydbTimestamp] — unsigned legacy types with JDBC vendor codes. */
class ForceLegacyStandardTemporalIT : BaseYdbTest() {

    object LegacyStdTemporal : YdbTable("force_legacy_std_temporal") {
        val id = integer("id")
        val dateCol = ydbDate("date_col")
        val dateTimeCol = ydbDatetime("datetime_col")
        val timestampCol = ydbTimestamp("timestamp_col")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = emptyList()

    @Test
    fun `should round-trip standard temporal columns as legacy types`() = tx {
        SchemaUtils.create(LegacyStdTemporal)

        val dateValue = LocalDate.of(2019, 12, 31)
        val dateTimeValue = LocalDateTime.of(2019, 12, 31, 23, 59, 59)
        val timestampValue = Instant.parse("2019-12-31T20:59:59Z")

        LegacyStdTemporal.insert {
            it[id] = 1
            it[dateCol] = dateValue
            it[dateTimeCol] = dateTimeValue
            it[timestampCol] = timestampValue
        }

        val row = LegacyStdTemporal.selectAll().single()
        assertEquals(dateValue, row[LegacyStdTemporal.dateCol])
        assertEquals(dateTimeValue, row[LegacyStdTemporal.dateTimeCol])
        assertEquals(timestampValue, row[LegacyStdTemporal.timestampCol])
    }

    @Test
    fun `should emit Date Datetime Timestamp ddl`() = tx {
        SchemaUtils.create(LegacyStdTemporal)

        val ddl = LegacyStdTemporal.ddl.joinToString(" ")
        assertTrue(ddl.contains("date_col Date") && !ddl.contains("Date32"), ddl)
        assertTrue(ddl.contains("datetime_col Datetime") && !ddl.contains("Datetime64"), ddl)
        assertTrue(ddl.contains("timestamp_col Timestamp") && !ddl.contains("Timestamp64"), ddl)
    }
}
