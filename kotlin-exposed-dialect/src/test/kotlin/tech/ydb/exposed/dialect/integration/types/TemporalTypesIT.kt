package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class TemporalTypesIT : BaseYdbTest() {

    object TemporalTypes : YdbTable("temporal_types") {
        val id = integer("id")
        val dateCol = date("date_col")
        val dateTimeCol = datetime("datetime_col")
        val timestampCol = timestamp("timestamp_col")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(TemporalTypes)

    @Test
    fun `should round-trip temporal types`() = tx {
        val dateValue = LocalDate.of(2026, 4, 13)
        val dateTimeValue = LocalDateTime.of(2026, 4, 13, 14, 30, 15)
        val timestampValue = Instant.parse("2026-04-13T11:30:15Z")

        TemporalTypes.insert {
            it[id] = 1
            it[dateCol] = dateValue
            it[dateTimeCol] = dateTimeValue
            it[timestampCol] = timestampValue
        }

        val row = TemporalTypes.selectAll().single()

        assertEquals(dateValue, row[TemporalTypes.dateCol])
        assertEquals(dateTimeValue, row[TemporalTypes.dateTimeCol])
        assertEquals(timestampValue, row[TemporalTypes.timestampCol])
    }
}