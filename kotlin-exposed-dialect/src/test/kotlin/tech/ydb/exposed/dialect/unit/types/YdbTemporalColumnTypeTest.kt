package tech.ydb.exposed.dialect.unit.types

import org.jetbrains.exposed.v1.core.Table
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.code.YdbJdbcCode
import tech.ydb.exposed.dialect.javatime.ydbDate
import tech.ydb.exposed.dialect.javatime.ydbDate32
import tech.ydb.exposed.dialect.javatime.ydbDatetime64
import tech.ydb.exposed.dialect.javatime.ydbTimestamp64
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class YdbTemporalColumnTypeTest {

    private object PlainTable : Table("temporal_columns") {
        val legacyDate = ydbDate("legacy_date")
        val signedDate = ydbDate32("date32")
    }

    private object TemporalColumnsTable : Table("ydb_temporal_columns") {
        val legacyDate = ydbDate("legacy_date")
        val signedDate = ydbDate32("date32")
        val signedDatetime = ydbDatetime64("datetime64")
        val signedTimestamp = ydbTimestamp64("timestamp64")
    }

    @Test
    fun `sqlType is derived from JDBC code for Table ydbDate`() {
        assertBinding(
            column = PlainTable.legacyDate,
            sqlType = "Date",
            vendorCode = YdbJdbcCode.DATE,
            value = LocalDate.of(2026, 4, 13)
        )
        assertEquals("Date32", PlainTable.signedDate.columnType.sqlType())
    }

    @Test
    fun `ydbDate and ydbDate32 use unsigned and signed types`() {
        assertEquals("Date", TemporalColumnsTable.legacyDate.columnType.sqlType())
        assertEquals("Date32", TemporalColumnsTable.signedDate.columnType.sqlType())
        assertEquals("Datetime64", TemporalColumnsTable.signedDatetime.columnType.sqlType())
        assertEquals("Timestamp64", TemporalColumnsTable.signedTimestamp.columnType.sqlType())
    }

    @Test
    fun `ydbDate32 binds Date32 vendor code`() {
        assertBinding(
            column = TemporalColumnsTable.signedDate,
            sqlType = "Date32",
            vendorCode = YdbJdbcCode.DATE32,
            value = LocalDate.of(2026, 4, 13)
        )
    }

    @Test
    fun `ydbDatetime64 binds Datetime64 vendor code`() {
        assertBinding(
            column = TemporalColumnsTable.signedDatetime,
            sqlType = "Datetime64",
            vendorCode = YdbJdbcCode.DATETIME64,
            value = LocalDateTime.of(2026, 4, 13, 14, 30, 15)
        )
    }

    @Test
    fun `ydbTimestamp64 binds Timestamp64 vendor code`() {
        assertBinding(
            column = TemporalColumnsTable.signedTimestamp,
            sqlType = "Timestamp64",
            vendorCode = YdbJdbcCode.TIMESTAMP64,
            value = Instant.parse("2026-04-13T11:30:15Z")
        )
    }

    private fun assertBinding(
        column: org.jetbrains.exposed.v1.core.Column<*>,
        sqlType: String,
        vendorCode: Int,
        value: Any
    ) {
        assertEquals(sqlType, column.columnType.sqlType())

        val (stmt, capture) = ydbPreparedStatementCapture()
        column.columnType.setParameter(stmt, 1, value)

        val actual = capture()
        assertNotNull(actual)
        assertEquals(BoundSqlObject(1, value, vendorCode), actual)
    }
}
