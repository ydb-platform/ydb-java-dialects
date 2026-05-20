package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbDialect
import tech.ydb.exposed.dialect.createYdbStatement
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.javatime.ydbDate32
import tech.ydb.exposed.dialect.javatime.ydbDatetime64
import tech.ydb.exposed.dialect.javatime.ydbTimestamp64
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Signed temporal columns with [tech.ydb.exposed.dialect.registerYdbDialect] `enableSignedDatetimes = true` and
 * `forceSignedDatetimes=true` on the JDBC URL (set explicitly in [jdbcUrlSuffix]).
 */
class SignedTemporalTypesIT : BaseYdbTest() {

    object SignedTemporal : Table("signed_temporal_types") {
        val id = integer("id")
        val dateCol = ydbDate32("date_col")
        val dateTimeCol = ydbDatetime64("datetime_col")
        val timestampCol = ydbTimestamp64("timestamp_col")

        override val primaryKey = PrimaryKey(id)

        override fun createStatement(): List<String> = createYdbStatement()
    }

    override val enableSignedDatetimes: Boolean = true

    override val tables: List<Table> = listOf(SignedTemporal)

    override val jdbcUrlSuffix: String = "&forceSignedDatetimes=true"

    @Test
    fun `registerYdbDialect wires signed dialect when forceSignedDatetimes in url`() {
        val dialect = db.dialect as YdbDialect
        assertTrue(dialect.enableSignedDatetimes)
    }

    @Test
    fun `should round-trip signed temporal columns`() = tx {
        val dateValue = LocalDate.of(2026, 5, 16)
        val dateTimeValue = LocalDateTime.of(2026, 5, 16, 14, 30, 15)
        val timestampValue = Instant.parse("2026-05-16T11:30:15Z")

        SignedTemporal.insert {
            it[id] = 1
            it[dateCol] = dateValue
            it[dateTimeCol] = dateTimeValue
            it[timestampCol] = timestampValue
        }

        val row = SignedTemporal.selectAll().single()
        assertEquals(dateValue, row[SignedTemporal.dateCol])
        assertEquals(dateTimeValue, row[SignedTemporal.dateTimeCol])
        assertEquals(timestampValue, row[SignedTemporal.timestampCol])
    }

    @Test
    fun `should emit Date32 Datetime64 Timestamp64 ddl`() = tx {
        SchemaUtils.create(SignedTemporal)

        val ddl = SignedTemporal.ddl.joinToString(" ")
        assertTrue(ddl.contains("date_col Date32"), ddl)
        assertTrue(ddl.contains("datetime_col Datetime64"), ddl)
        assertTrue(ddl.contains("timestamp_col Timestamp64"), ddl)
    }
}
