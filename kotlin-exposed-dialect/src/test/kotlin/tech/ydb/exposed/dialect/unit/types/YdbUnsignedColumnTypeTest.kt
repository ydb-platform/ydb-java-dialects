package tech.ydb.exposed.dialect.unit.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.code.YdbJdbcCode
import tech.ydb.exposed.dialect.YdbUByteColumnType
import tech.ydb.exposed.dialect.YdbUIntegerColumnType
import tech.ydb.exposed.dialect.YdbULongColumnType
import tech.ydb.exposed.dialect.YdbUShortColumnType

class YdbUnsignedColumnTypeTest {

    @Test
    fun `ubyte binds Uint8 vendor code`() {
        val type = YdbUByteColumnType()
        val (stmt, capture) = ydbPreparedStatementCapture()

        type.setParameter(stmt, 1, 42.toUByte())

        val bound = capture()!!
        assertEquals(1, bound.index)
        assertEquals(42.toShort(), bound.value)
        assertEquals(YdbJdbcCode.UINT8, bound.targetSqlType)
    }

    @Test
    fun `ushort binds Uint16 vendor code`() {
        val type = YdbUShortColumnType()
        val (stmt, capture) = ydbPreparedStatementCapture()

        type.setParameter(stmt, 1, 1000.toUShort())

        val bound = capture()!!
        assertEquals(1, bound.index)
        assertEquals(1000, bound.value)
        assertEquals(YdbJdbcCode.UINT16, bound.targetSqlType)
    }

    @Test
    fun `uint32 binds Uint32 vendor code`() {
        val type = YdbUIntegerColumnType()
        val (stmt, capture) = ydbPreparedStatementCapture()

        type.setParameter(stmt, 1, 3_000_000_000u)

        assertEquals(BoundSqlObject(1, 3_000_000_000L, YdbJdbcCode.UINT32), capture())
    }

    @Test
    fun `ulong binds Uint64 vendor code`() {
        val type = YdbULongColumnType()
        val (stmt, capture) = ydbPreparedStatementCapture()

        type.setParameter(stmt, 1, 42uL)

        assertEquals(BoundSqlObject(1, 42L, YdbJdbcCode.UINT64), capture())
    }
}
