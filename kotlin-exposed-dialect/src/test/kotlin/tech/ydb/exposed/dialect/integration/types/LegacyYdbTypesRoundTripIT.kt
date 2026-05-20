package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.createYdbStatement
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.javatime.ydbDate
import tech.ydb.exposed.dialect.javatime.ydbDatetime
import tech.ydb.exposed.dialect.ydbInterval
import tech.ydb.exposed.dialect.javatime.ydbTimestamp
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/** Round-trip for legacy YDB temporal/interval types (`Date`, `Datetime`, `Timestamp`, `Interval`). */
class LegacyYdbTypesRoundTripIT : BaseYdbTest() {

    object LegacyTypes : Table("legacy_ydb_types_round_trip") {
        val id = integer("id")
        val dateCol = ydbDate("date_col")
        val dateTimeCol = ydbDatetime("datetime_col")
        val timestampCol = ydbTimestamp("timestamp_col")
        val intervalCol = ydbInterval("interval_col")

        override val primaryKey = PrimaryKey(id)

        override fun createStatement(): List<String> = createYdbStatement()
    }

    override val tables: List<Table> = listOf(LegacyTypes)

    @Test
    fun `should round-trip legacy Date Datetime Timestamp Interval`() = tx {
        val dateValue = LocalDate.of(2020, 6, 15)
        val dateTimeValue = LocalDateTime.of(2020, 6, 15, 12, 30, 0)
        val timestampValue = Instant.parse("2020-06-15T09:30:00Z")
        val duration = Duration.ofHours(48).plusMinutes(15)

        LegacyTypes.insert {
            it[id] = 1
            it[dateCol] = dateValue
            it[dateTimeCol] = dateTimeValue
            it[timestampCol] = timestampValue
            it[intervalCol] = duration
        }

        val row = LegacyTypes.selectAll().single()

        assertEquals(dateValue, row[LegacyTypes.dateCol])
        assertEquals(dateTimeValue, row[LegacyTypes.dateTimeCol])
        assertEquals(timestampValue, row[LegacyTypes.timestampCol])
        assertEquals(duration, row[LegacyTypes.intervalCol])
    }
}
