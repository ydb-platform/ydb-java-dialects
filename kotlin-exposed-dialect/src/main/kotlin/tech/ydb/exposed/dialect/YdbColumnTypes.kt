package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcPreparedStatementImpl
import tech.ydb.exposed.dialect.code.YdbJdbcCode
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.PreparedStatement
import java.time.Duration
import java.util.UUID
import kotlin.UByte
import kotlin.UInt
import kotlin.ULong
import kotlin.UShort

// region Table column extensions

fun Table.ydbDecimal(name: String, precision: Int, scale: Int): Column<BigDecimal> =
    registerColumn(name, YdbDecimalColumnType(precision, scale))

fun Table.ydbJson(name: String): Column<String> =
    registerColumn(name, YdbJsonStringColumnType())

fun Table.ydbJsonDocument(name: String): Column<String> =
    registerColumn(name, YdbJsonDocumentStringColumnType())

fun Table.ydbUuid(name: String): Column<UUID> =
    registerColumn(name, YdbUuidColumnType())

fun Table.ydbUint64(name: String): Column<Long> =
    registerColumn(name, YdbUint64ColumnType())

fun Table.ydbUbyte(name: String): Column<UByte> =
    registerColumn(name, YdbUByteColumnType())

fun Table.ydbUshort(name: String): Column<UShort> =
    registerColumn(name, YdbUShortColumnType())

fun Table.ydbUint32(name: String): Column<UInt> =
    registerColumn(name, YdbUIntegerColumnType())

fun Table.ydbUlong(name: String): Column<ULong> =
    registerColumn(name, YdbULongColumnType())

/** Legacy unsigned YDB `Interval` ([Duration]). */
fun Table.ydbInterval(name: String): Column<Duration> =
    registerColumn(name, YdbIntervalColumnType(YdbJdbcCode.INTERVAL))

/** Extended signed YDB `Interval64` ([Duration]). */
fun Table.ydbInterval64(name: String): Column<Duration> =
    registerColumn(name, YdbIntervalColumnType(YdbJdbcCode.INTERVAL64))

fun ydbDecimalLiteral(
    value: BigDecimal,
    precision: Int,
    scale: Int
): Expression<BigDecimal> = YdbDecimalLiteral(value, precision, scale)

// endregion

// region JDBC bind helper

internal fun bindYdbParameter(
    stmt: PreparedStatementApi,
    index: Int,
    value: Any?,
    targetSqlType: Int,
    columnType: IColumnType<*>
) {
    if (value == null) {
        stmt.setNull(index, columnType)
        return
    }

    val jdbcStatement = (stmt as? JdbcPreparedStatementImpl)?.statement
    if (jdbcStatement is PreparedStatement) {
        jdbcStatement.setObject(index, value, targetSqlType)
    }
}

// endregion

// region Column types

internal class YdbDecimalColumnType(
    private val precision: Int,
    private val scale: Int
) : ColumnType<BigDecimal>() {

    private val targetSqlType: Int = YdbJdbcCode.decimal(precision, scale)

    init {
        require(precision in 1..35) { "YDB Decimal precision must be in 1..35" }
        require(scale in 0..precision) { "YDB Decimal scale must be in 0..precision" }
    }

    override fun sqlType(): String = "Decimal($precision, $scale)"

    override fun valueFromDB(value: Any): BigDecimal = when (value) {
        is BigDecimal -> value
        is String -> value.toBigDecimal()
        else -> error("Unexpected value for Decimal: $value of ${value::class}")
    }

    override fun notNullValueToDB(value: BigDecimal): Any = normalizeScale(value)

    override fun nonNullValueToString(value: BigDecimal): String = normalizeScale(value).toPlainString()

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val dbValue = value?.let { normalizeScale(it as BigDecimal) }
        bindYdbParameter(stmt, index, dbValue, targetSqlType, this)
    }

    private fun normalizeScale(value: BigDecimal): BigDecimal {
        require(value.scale() <= scale) {
            "YDB Decimal value $value has scale ${value.scale()}, which exceeds column scale $scale"
        }
        return value.setScale(scale)
    }
}

internal abstract class YdbTypedStringColumnType(
    private val ydbSqlType: String,
    private val targetSqlType: Int
) : ColumnType<String>() {
    override fun sqlType(): String = ydbSqlType

    override fun valueFromDB(value: Any): String = value.toString()

    override fun notNullValueToDB(value: String): Any = value

    override fun nonNullValueToString(value: String): String =
        "'${value.replace("'", "''")}'"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        bindYdbParameter(stmt, index, value, targetSqlType, this)
    }
}

internal class YdbJsonStringColumnType : YdbTypedStringColumnType("Json", YdbJdbcCode.JSON)

internal class YdbJsonDocumentStringColumnType : YdbTypedStringColumnType("JsonDocument", YdbJdbcCode.JSON_DOCUMENT)

internal class YdbUuidColumnType : ColumnType<UUID>() {
    override fun sqlType(): String = "Uuid"

    override fun valueFromDB(value: Any): UUID = when (value) {
        is UUID -> value
        is String -> UUID.fromString(value)
        else -> error("Unexpected value for native UUID: $value of ${value::class}")
    }

    override fun notNullValueToDB(value: UUID): Any = value

    override fun nonNullValueToString(value: UUID): String = "Uuid(\"$value\")"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        bindYdbParameter(stmt, index, value, YdbJdbcCode.UUID, this)
    }
}

internal class YdbUint64ColumnType : ColumnType<Long>() {
    override fun sqlType(): String = "Uint64"

    override fun valueFromDB(value: Any): Long = when (value) {
        is Long -> {
            require(value >= 0) { "Uint64 value cannot be negative: $value" }
            value
        }

        is Int -> {
            require(value >= 0) { "Uint64 value cannot be negative: $value" }
            value.toLong()
        }

        is BigInteger -> value.toLongCompatibleUint64()
        is String -> value.toBigInteger().toLongCompatibleUint64()
        else -> error("Unexpected value for Uint64: $value of ${value::class}")
    }

    override fun notNullValueToDB(value: Long): Any {
        require(value >= 0) { "Uint64 column cannot store negative value: $value" }
        return value
    }

    override fun nonNullValueToString(value: Long): String {
        require(value >= 0) { "Uint64 column cannot store negative value: $value" }
        return value.toString()
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        bindYdbParameter(stmt, index, value, YdbJdbcCode.UINT64, this)
    }
}

internal class YdbUByteColumnType : ColumnType<UByte>() {
    override fun sqlType(): String = "Uint8"

    override fun valueFromDB(value: Any): UByte = when (value) {
        is UByte -> value
        is Byte -> value.toUByte()
        is Short -> value.toUByte()
        is Int -> value.toUByte()
        is Number -> value.toInt().toUByte()
        is String -> value.toUByte()
        else -> error("Unexpected value for Uint8: $value of ${value::class}")
    }

    override fun notNullValueToDB(value: UByte): Any = value.toShort()

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val dbValue = when (value) {
            null -> null
            is UByte -> value.toShort()
            is Number -> value.toShort()
            else -> error("Unexpected bind value for Uint8: $value of ${value::class}")
        }
        bindYdbParameter(stmt, index, dbValue, YdbJdbcCode.UINT8, this)
    }
}

internal class YdbUShortColumnType : ColumnType<UShort>() {
    override fun sqlType(): String = "Uint16"

    override fun valueFromDB(value: Any): UShort = when (value) {
        is UShort -> value
        is Short -> value.toUShort()
        is Int -> value.toUShort()
        is Number -> value.toInt().toUShort()
        is String -> value.toUShort()
        else -> error("Unexpected value for Uint16: $value of ${value::class}")
    }

    override fun notNullValueToDB(value: UShort): Any = value.toInt()

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val dbValue = when (value) {
            null -> null
            is UShort -> value.toInt()
            is Number -> value.toInt()
            else -> error("Unexpected bind value for Uint16: $value of ${value::class}")
        }
        bindYdbParameter(stmt, index, dbValue, YdbJdbcCode.UINT16, this)
    }
}

internal class YdbUIntegerColumnType : ColumnType<UInt>() {
    override fun sqlType(): String = "Uint32"

    override fun valueFromDB(value: Any): UInt = when (value) {
        is UInt -> value
        is Int -> value.toUInt()
        is Long -> value.toUInt()
        is Number -> value.toLong().toUInt()
        is String -> value.toUInt()
        else -> error("Unexpected value for Uint32: $value of ${value::class}")
    }

    override fun notNullValueToDB(value: UInt): Any = value.toLong()

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val dbValue = when (value) {
            null -> null
            is UInt -> value.toLong()
            is Number -> value.toLong()
            else -> error("Unexpected bind value for Uint32: $value of ${value::class}")
        }
        bindYdbParameter(stmt, index, dbValue, YdbJdbcCode.UINT32, this)
    }
}

internal class YdbULongColumnType : ColumnType<ULong>() {
    override fun sqlType(): String = "Uint64"

    override fun valueFromDB(value: Any): ULong = when (value) {
        is ULong -> value
        is Long -> value.toULong()
        is Int -> value.toULong()
        is Number -> value.toLong().toULong()
        is String -> value.toULong()
        else -> error("Unexpected value for Uint64: $value of ${value::class}")
    }

    override fun notNullValueToDB(value: ULong): Any {
        val longValue = value.toLong()
        require(longValue >= 0) { "Uint64 column cannot store negative value: $value" }
        return longValue
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val dbValue = when (value) {
            is ULong -> value.toLong()
            else -> value
        }
        bindYdbParameter(stmt, index, dbValue, YdbJdbcCode.UINT64, this)
    }
}

internal class YdbIntervalColumnType(
    private val jdbcTypeCode: Int
) : ColumnType<Duration>() {
    override fun sqlType(): String =
        if (jdbcTypeCode == YdbJdbcCode.INTERVAL) "Interval" else "Interval64"

    override fun valueFromDB(value: Any): Duration = when (value) {
        is Duration -> value
        is String -> Duration.parse(value)
        else -> error("Unexpected value for Interval: $value of ${value::class}")
    }

    override fun notNullValueToDB(value: Duration): Any = value

    override fun nonNullValueToString(value: Duration): String = "'$value'"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        bindYdbParameter(stmt, index, value, jdbcTypeCode, this)
    }
}

internal class YdbDecimalLiteral(
    private val value: BigDecimal,
    private val precision: Int,
    private val scale: Int
) : Expression<BigDecimal>() {

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        require(value.scale() <= scale) {
            "Decimal value $value has scale ${value.scale()}, which exceeds the allowed scale $scale"
        }
        val normalized = value.setScale(scale).toPlainString()
        queryBuilder.append("""Decimal("$normalized", $precision, $scale)""")
    }
}

private fun BigInteger.toLongCompatibleUint64(): Long {
    require(this >= BigInteger.ZERO) { "Uint64 value cannot be negative: $this" }
    require(this <= BigInteger.valueOf(Long.MAX_VALUE)) {
        "Uint64 value $this exceeds Long-backed Uint64 range (0..${Long.MAX_VALUE})"
    }
    return toLong()
}

// endregion
