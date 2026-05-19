package tech.ydb.exposed.dialect.unit.types

import org.jetbrains.exposed.v1.core.Table
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.ydbInterval
import tech.ydb.exposed.dialect.ydbInterval64
import tech.ydb.exposed.dialect.code.YdbJdbcCode
import java.time.Duration

class YdbIntervalColumnTypeTest {

    private object IntervalColumns : YdbTable("interval_columns") {
        val legacy = ydbInterval("legacy")
        val extended = ydbInterval64("extended")
    }

    @Test
    fun `legacy ydbInterval uses Interval sql type and vendor code`() {
        assertEquals("Interval", IntervalColumns.legacy.columnType.sqlType())
        assertBinding(IntervalColumns.legacy, YdbJdbcCode.INTERVAL)
    }

    @Test
    fun `ydbInterval64 uses Interval64 sql type and vendor code`() {
        assertEquals("Interval64", IntervalColumns.extended.columnType.sqlType())
        assertBinding(IntervalColumns.extended, YdbJdbcCode.INTERVAL64)
    }

    private fun assertBinding(
        column: org.jetbrains.exposed.v1.core.Column<*>,
        expectedVendorCode: Int
    ) {
        val duration = Duration.ofMinutes(90)
        val (stmt, capture) = ydbPreparedStatementCapture()

        column.columnType.setParameter(stmt, 1, duration)

        val actual = capture()
        assertNotNull(actual)
        assertEquals(BoundSqlObject(1, duration, expectedVendorCode), actual)
    }
}
