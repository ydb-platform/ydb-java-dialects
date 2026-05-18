package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import tech.ydb.exposed.dialect.connectYdb
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.ydbTransaction
import tech.ydb.test.junit5.YdbHelperExtension
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import tech.ydb.exposed.dialect.javatime.ydbDate
import tech.ydb.exposed.dialect.javatime.ydbDatetime
import tech.ydb.exposed.dialect.javatime.ydbTimestamp

/**
 * [ydbDate] / [ydbDatetime] / [ydbTimestamp] — unsigned legacy types with JDBC vendor codes.
 */
class ForceLegacyStandardTemporalIT {

    object LegacyStdTemporal : YdbTable("force_legacy_std_temporal") {
        val id = integer("id")
        val dateCol = ydbDate("date_col")
        val dateTimeCol = ydbDatetime("datetime_col")
        val timestampCol = ydbTimestamp("timestamp_col")

        override val primaryKey = PrimaryKey(id)
    }

    private lateinit var db: Database

    @BeforeEach
    fun setUp() {
        val jdbcUrl = buildString {
            append("jdbc:ydb:")
            append(if (ydb.useTls()) "grpcs://" else "grpc://")
            append(ydb.endpoint())
            append(ydb.database())
            append("?disablePrepareDataQuery=true")
            ydb.authToken()?.let { append("&token=").append(it) }
        }
        db = connectYdb(url = jdbcUrl)
    }

    @AfterEach
    fun tearDown() {
        if (!::db.isInitialized) return

        runCatching {
            ydbTransaction(db) {
                SchemaUtils.drop(LegacyStdTemporal)
            }
        }
        runCatching { TransactionManager.closeAndUnregister(db) }
    }

    @Test
    fun `should round-trip standard temporal columns as legacy types`() = ydbTransaction(db) {
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
    fun `should emit Date Datetime Timestamp ddl`() = ydbTransaction(db) {
        SchemaUtils.create(LegacyStdTemporal)

        val ddl = LegacyStdTemporal.ddl.joinToString(" ")
        assertTrue(ddl.contains("date_col Date") && !ddl.contains("Date32"), ddl)
        assertTrue(ddl.contains("datetime_col Datetime") && !ddl.contains("Datetime64"), ddl)
        assertTrue(ddl.contains("timestamp_col Timestamp") && !ddl.contains("Timestamp64"), ddl)
    }

    companion object {
        @JvmField
        @RegisterExtension
        val ydb: YdbHelperExtension = YdbHelperExtension()
    }
}
