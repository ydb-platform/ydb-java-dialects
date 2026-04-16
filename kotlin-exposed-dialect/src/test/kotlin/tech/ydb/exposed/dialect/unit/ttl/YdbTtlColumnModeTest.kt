package tech.ydb.exposed.dialect.unit.ttl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTtlColumnMode

class YdbTtlColumnModeTest {

    @Test
    fun `should map date type mode to null suffix`() {
        assertNull(YdbTtlColumnMode.DATE_TYPE.toSql())
    }

    @Test
    fun `should map numeric ttl modes to sql suffix`() {
        assertEquals("SECONDS", YdbTtlColumnMode.SECONDS.toSql())
        assertEquals("MILLISECONDS", YdbTtlColumnMode.MILLISECONDS.toSql())
        assertEquals("MICROSECONDS", YdbTtlColumnMode.MICROSECONDS.toSql())
        assertEquals("NANOSECONDS", YdbTtlColumnMode.NANOSECONDS.toSql())
    }
}