package tech.ydb.exposed.dialect.unit.types

import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbDataTypeProvider

/**
 * [YdbDataTypeProvider.hexToDb] formats binary data for **inline SQL literals**, not for JDBC bind.
 *
 * Exposed calls it from [org.jetbrains.exposed.v1.core.BlobColumnType.nonNullValueToString]
 * ([LiteralOp], inline UPSERT values, etc.).
 * YDB `Bytes` is a [String](https://ydb.tech/docs/en/yql/reference/types/primitive) alias; literals use
 * [String::HexDecode](https://ydb.tech/docs/en/yql/reference/udf/list/string).
 */
class YdbHexToDbTest {

    private val provider = YdbDataTypeProvider()

    @Test
    fun `uses String HexDecode without cast`() {
        assertEquals("String::HexDecode('deadbeef')", provider.hexToDb("deadbeef"))
        assertEquals("String::HexDecode('')", provider.hexToDb(""))
    }

    @Test
    fun `matches ExposedBlob hexString output`() {
        val bytes = byteArrayOf(0x01, 0x02, 0xAB.toByte(), 0xCD.toByte())
        val hex = ExposedBlob(bytes).hexString()

        assertEquals("0102abcd", hex)
        assertEquals("String::HexDecode('0102abcd')", provider.hexToDb(hex))
    }
}
