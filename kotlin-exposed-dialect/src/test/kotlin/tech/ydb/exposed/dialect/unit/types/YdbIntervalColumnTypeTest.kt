package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.types.YdbIntervalColumnType
import java.time.Duration

class YdbIntervalColumnTypeTest {

    private val type = YdbIntervalColumnType()

    @Test
    fun `should return interval sql type`() {
        assertEquals("Interval", type.sqlType())
    }

    @Test
    fun `should parse interval from db`() {
        val duration = Duration.ofHours(1).plusMinutes(2).plusSeconds(3)

        assertEquals(duration, type.valueFromDB(duration))
        assertEquals(duration, type.valueFromDB("PT1H2M3S"))
    }

    @Test
    fun `should convert interval to db`() {
        val duration = Duration.ofMinutes(90)

        assertEquals(duration, type.notNullValueToDB(duration))
        assertEquals("'PT1H30M'", type.nonNullValueToString(duration))
    }
}