package tech.ydb.exposed.dialect.types

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import java.math.BigDecimal
import java.time.Duration
import java.util.UUID

class YdbDecimalColumnType(
    private val precision: Int,
    private val scale: Int
) : ColumnType<BigDecimal>() {

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

    override fun notNullValueToDB(value: BigDecimal): Any =
        value.setScale(scale)

    override fun nonNullValueToString(value: BigDecimal): String =
        value.setScale(scale).toPlainString()
}

class YdbIntervalColumnType : ColumnType<Duration>() {
    override fun sqlType(): String = "Interval"

    override fun valueFromDB(value: Any): Duration = when (value) {
        is Duration -> value
        is String -> Duration.parse(value)
        else -> error("Unexpected value for Interval: $value of ${value::class}")
    }

    override fun notNullValueToDB(value: Duration): Any = value

    override fun nonNullValueToString(value: Duration): String =
        "'${value}'"
}

class YdbJsonStringColumnType : ColumnType<String>() {
    override fun sqlType(): String = "Json"

    override fun valueFromDB(value: Any): String = value.toString()

    override fun notNullValueToDB(value: String): Any = value

    override fun nonNullValueToString(value: String): String =
        "'${value.replace("'", "''")}'"
}

class YdbUuidAsUtf8ColumnType : ColumnType<UUID>() {
    override fun sqlType(): String = "Utf8"

    override fun valueFromDB(value: Any): UUID = when (value) {
        is UUID -> value
        is String -> UUID.fromString(value)
        else -> error("Unexpected value for UUID(Utf8): $value of ${value::class}")
    }

    override fun notNullValueToDB(value: UUID): Any = value.toString()

    override fun nonNullValueToString(value: UUID): String =
        "'$value'"
}

class YdbUuidAsStringColumnType : ColumnType<UUID>() {
    override fun sqlType(): String = "String"

    override fun valueFromDB(value: Any): UUID = when (value) {
        is UUID -> value
        is ByteArray -> UUID.fromString(value.toString(Charsets.UTF_8))
        is String -> UUID.fromString(value)
        else -> error("Unexpected value for UUID(String): $value of ${value::class}")
    }

    override fun notNullValueToDB(value: UUID): Any = value.toString().toByteArray(Charsets.UTF_8)

    override fun nonNullValueToString(value: UUID): String =
        "'$value'"
}

class YdbUuidColumnType : ColumnType<UUID>() {
    override fun sqlType(): String = "Uuid"

    override fun valueFromDB(value: Any): UUID = when (value) {
        is UUID -> value
        is String -> UUID.fromString(value)
        else -> error("Unexpected value for native UUID: $value of ${value::class}")
    }

    override fun notNullValueToDB(value: UUID): Any =
        value.toString()

    override fun nonNullValueToString(value: UUID): String =
        "'$value'"
}

class YdbUint64ColumnType : ColumnType<Long>() {
    override fun sqlType(): String = "Uint64"

    override fun valueFromDB(value: Any): Long = when (value) {
        is Long -> value
        is Int -> value.toLong()
        is Number -> value.toLong()
        is String -> value.toLong()
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
}

fun Table.ydbDecimal(name: String, precision: Int, scale: Int): Column<BigDecimal> =
    registerColumn(name, YdbDecimalColumnType(precision, scale))

fun Table.ydbInterval(name: String): Column<Duration> =
    registerColumn(name, YdbIntervalColumnType())

fun Table.ydbJson(name: String): Column<String> =
    registerColumn(name, YdbJsonStringColumnType())

fun Table.ydbUuidUtf8(name: String): Column<UUID> =
    registerColumn(name, YdbUuidAsUtf8ColumnType())

fun Table.ydbUuidBytes(name: String): Column<UUID> =
    registerColumn(name, YdbUuidAsStringColumnType())

fun Table.ydbUuid(name: String): Column<UUID> =
    registerColumn(name, YdbUuidColumnType())

fun Table.ydbUint64(name: String): Column<Long> =
    registerColumn(name, YdbUint64ColumnType())