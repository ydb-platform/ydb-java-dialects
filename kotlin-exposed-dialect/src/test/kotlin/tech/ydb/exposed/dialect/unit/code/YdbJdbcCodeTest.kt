package tech.ydb.exposed.dialect.unit.code

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.code.YdbJdbcCode

class YdbJdbcCodeTest {

    @Test
    fun `primitive codes match JDBC driver layout`() {
        assertEquals(10_025, YdbJdbcCode.DATE32)
        assertEquals(10_026, YdbJdbcCode.DATETIME64)
        assertEquals(10_027, YdbJdbcCode.TIMESTAMP64)
        assertEquals(10_028, YdbJdbcCode.INTERVAL64)
        assertEquals(10_014, YdbJdbcCode.JSON)
        assertEquals(10_016, YdbJdbcCode.DATE)
        assertEquals(10_019, YdbJdbcCode.INTERVAL)
    }

    @Test
    fun `decimal code encodes precision and scale`() {
        assertEquals(YdbJdbcCode.SQL_KIND_DECIMAL + (10 shl 6) + 2, YdbJdbcCode.decimal(10, 2))
    }
}
