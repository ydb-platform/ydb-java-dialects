package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.ydbDecimal
import tech.ydb.exposed.dialect.ydbInterval64
import tech.ydb.exposed.dialect.ydbJson
import tech.ydb.exposed.dialect.ydbJsonDocument
import tech.ydb.exposed.dialect.ydbUint64
import tech.ydb.exposed.dialect.ydbUbyte
import tech.ydb.exposed.dialect.ydbUint32
import tech.ydb.exposed.dialect.ydbUlong
import tech.ydb.exposed.dialect.ydbUshort
import tech.ydb.exposed.dialect.ydbUuid
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import tech.ydb.exposed.dialect.javatime.ydbDate
import tech.ydb.exposed.dialect.javatime.ydbDate32
import tech.ydb.exposed.dialect.javatime.ydbDatetime
import tech.ydb.exposed.dialect.javatime.ydbDatetime64
import tech.ydb.exposed.dialect.javatime.ydbTimestamp
import tech.ydb.exposed.dialect.javatime.ydbTimestamp64

/**
 * End-to-end insert/select coverage for every type mapping implemented in this dialect.
 *
 * Not covered here (no public column API yet): `Yson`, `TzDate`, `TzDatetime`, `TzTimestamp`.
 */
class AllTypesRoundTripIT : BaseYdbTest() {

    object ScalarTypes : YdbTable("all_types_scalars") {
        val id = integer("id")
        val byteCol = byte("byte_col")
        val ubyteCol = ydbUbyte("ubyte_col")
        val shortCol = short("short_col")
        val ushortCol = ydbUshort("ushort_col")
        val intCol = integer("int_col")
        val uintCol = ydbUint32("uint_col")
        val longCol = long("long_col")
        val ulongCol = ydbUlong("ulong_col")
        val boolCol = bool("bool_col")
        val floatCol = float("float_col")
        val doubleCol = double("double_col")
        val varcharCol = varchar("varchar_col", 255)
        val textCol = text("text_col")
        val binaryCol = binary("binary_col")

        override val primaryKey = PrimaryKey(id)
    }

    object StandardTemporal : YdbTable("all_types_std_temporal") {
        val id = integer("id")
        val dateCol = ydbDate("date_col")
        val dateTimeCol = ydbDatetime("datetime_col")
        val timestampCol = ydbTimestamp("timestamp_col")

        override val primaryKey = PrimaryKey(id)
    }

    object YdbExtensionTypes : YdbTable("all_types_ydb_ext") {
        val id = integer("id")
        val amount = ydbDecimal("amount", 12, 4)
        val jsonCol = ydbJson("json_col")
        val jsonDocCol = ydbJsonDocument("json_doc_col")
        val uuidCol = ydbUuid("uuid_col")
        val uint64Col = ydbUint64("uint64_col")
        val date32Col = ydbDate32("date32_col")
        val datetime64Col = ydbDatetime64("datetime64_col")
        val timestamp64Col = ydbTimestamp64("timestamp64_col")
        val interval64Col = ydbInterval64("interval64_col")

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(ScalarTypes, StandardTemporal, YdbExtensionTypes)

    @Test
    fun `should round-trip standard scalar and unsigned types`() = tx {
        val bytes = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())

        ScalarTypes.insert {
            it[id] = 1
            it[byteCol] = 42
            it[ubyteCol] = 255.toUByte()
            it[shortCol] = -32000
            it[ushortCol] = 65000.toUShort()
            it[intCol] = -2_000_000_000
            it[uintCol] = 3_000_000_000u
            it[longCol] = -9_000_000_000_000_000_000L
            it[ulongCol] = 9_223_372_036_854_775_807u
            it[boolCol] = true
            it[floatCol] = 1.25f
            it[doubleCol] = 3.141592653589793
            it[varcharCol] = "varchar-α"
            it[textCol] = "text-β\nline2"
            it[binaryCol] = bytes
        }

        val row = ScalarTypes.selectAll().single()

        assertEquals(42.toByte(), row[ScalarTypes.byteCol])
        assertEquals(255.toUByte(), row[ScalarTypes.ubyteCol])
        assertEquals((-32000).toShort(), row[ScalarTypes.shortCol])
        assertEquals(65000.toUShort(), row[ScalarTypes.ushortCol])
        assertEquals(-2_000_000_000, row[ScalarTypes.intCol])
        assertEquals(3_000_000_000u, row[ScalarTypes.uintCol])
        assertEquals(-9_000_000_000_000_000_000L, row[ScalarTypes.longCol])
        assertEquals(9_223_372_036_854_775_807u, row[ScalarTypes.ulongCol])
        assertEquals(true, row[ScalarTypes.boolCol])
        assertEquals(1.25f, row[ScalarTypes.floatCol])
        assertEquals(3.141592653589793, row[ScalarTypes.doubleCol])
        assertEquals("varchar-α", row[ScalarTypes.varcharCol])
        assertEquals("text-β\nline2", row[ScalarTypes.textCol])
        assertArrayEquals(bytes, row[ScalarTypes.binaryCol])
    }

    @Test
    fun `should round-trip standard Exposed java-time columns (Date Datetime Timestamp)`() = tx {
        val dateValue = LocalDate.of(2026, 5, 16)
        val dateTimeValue = LocalDateTime.of(2026, 5, 16, 18, 45, 30)
        val timestampValue = Instant.parse("2026-05-16T15:45:30Z")

        StandardTemporal.insert {
            it[id] = 1
            it[dateCol] = dateValue
            it[dateTimeCol] = dateTimeValue
            it[timestampCol] = timestampValue
        }

        val row = StandardTemporal.selectAll().single()

        assertEquals(dateValue, row[StandardTemporal.dateCol])
        assertEquals(dateTimeValue, row[StandardTemporal.dateTimeCol])
        assertEquals(timestampValue, row[StandardTemporal.timestampCol])
    }

    @Test
    fun `should round-trip YDB-specific column extensions`() = tx {
        val uuid = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
        val json = """{"k":"v","n":1}"""
        val jsonDoc = """{"doc":true}"""
        val duration = Duration.ofDays(2).plusHours(5).plusMinutes(7)

        YdbExtensionTypes.insert {
            it[id] = 1
            it[amount] = BigDecimal("9999.1234")
            it[jsonCol] = json
            it[jsonDocCol] = jsonDoc
            it[uuidCol] = uuid
            it[uint64Col] = 9_223_372_036_854_775_807L
            it[date32Col] = LocalDate.of(2030, 1, 2)
            it[datetime64Col] = LocalDateTime.of(2030, 1, 2, 3, 4, 5)
            it[timestamp64Col] = Instant.parse("2030-01-02T00:04:05Z")
            it[interval64Col] = duration
        }

        val row = YdbExtensionTypes.selectAll().single()

        assertEquals(BigDecimal("9999.1234"), row[YdbExtensionTypes.amount].setScale(4))
        assertEquals(json, row[YdbExtensionTypes.jsonCol])
        assertTrue(row[YdbExtensionTypes.jsonDocCol].contains("\"doc\":true"))
        assertEquals(uuid, row[YdbExtensionTypes.uuidCol])
        assertEquals(9_223_372_036_854_775_807L, row[YdbExtensionTypes.uint64Col])
        assertEquals(LocalDate.of(2030, 1, 2), row[YdbExtensionTypes.date32Col])
        assertEquals(LocalDateTime.of(2030, 1, 2, 3, 4, 5), row[YdbExtensionTypes.datetime64Col])
        assertEquals(Instant.parse("2030-01-02T00:04:05Z"), row[YdbExtensionTypes.timestamp64Col])
        assertEquals(duration, row[YdbExtensionTypes.interval64Col])
    }

    @Test
    fun `should emit expected ddl for scalar mappings`() = tx {
        val ddl = ScalarTypes.ddl.joinToString(" ")

        assertTrue(ddl.contains("byte_col Int8"))
        assertTrue(ddl.contains("ubyte_col Uint8"))
        assertTrue(ddl.contains("short_col Int16"))
        assertTrue(ddl.contains("ushort_col Uint16"))
        assertTrue(ddl.contains("int_col Int32"))
        assertTrue(ddl.contains("uint_col Uint32"))
        assertTrue(ddl.contains("long_col Int64"))
        assertTrue(ddl.contains("ulong_col Uint64"))
        assertTrue(ddl.contains("bool_col Bool"))
        assertTrue(ddl.contains("float_col Float"))
        assertTrue(ddl.contains("double_col Double"))
        assertTrue(ddl.contains("varchar_col Text"))
        assertTrue(ddl.contains("text_col Text"))
        assertTrue(ddl.contains("binary_col Bytes"))
    }

    @Test
    fun `should emit expected ddl for standard temporal and ydb extensions`() = tx {
        val stdDdl = StandardTemporal.ddl.joinToString(" ")
        assertTrue(stdDdl.contains("date_col Date32"))
        assertTrue(stdDdl.contains("datetime_col Datetime64"))
        assertTrue(stdDdl.contains("timestamp_col Timestamp64"))

        val extDdl = YdbExtensionTypes.ddl.joinToString(" ")
        assertTrue(extDdl.contains("amount Decimal(12, 4)"))
        assertTrue(extDdl.contains("json_col Json"))
        assertTrue(extDdl.contains("json_doc_col JsonDocument"))
        assertTrue(extDdl.contains("uuid_col Uuid"))
        assertTrue(extDdl.contains("uint64_col Uint64"))
        assertTrue(extDdl.contains("date32_col Date32"))
        assertTrue(extDdl.contains("datetime64_col Datetime64"))
        assertTrue(extDdl.contains("timestamp64_col Timestamp64"))
        assertTrue(extDdl.contains("interval64_col Interval64"))
    }
}
