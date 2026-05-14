package tech.ydb.exposed.dialect.types

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcPreparedStatementImpl
import tech.ydb.jdbc.YdbPreparedStatement
import tech.ydb.table.values.PrimitiveType
import java.math.BigDecimal
import java.math.BigInteger
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
        normalizeScale(value)

    override fun nonNullValueToString(value: BigDecimal): String =
        normalizeScale(value).toPlainString()

    private fun normalizeScale(value: BigDecimal): BigDecimal {
        require(value.scale() <= scale) {
            "YDB Decimal value $value has scale ${value.scale()}, which exceeds column scale $scale"
        }
        return value.setScale(scale)
    }
}

class YdbIntervalColumnType : ColumnType<Duration>() {
    override fun sqlType(): String = "Interval"

    override fun valueFromDB(value: Any): Duration = when (value) {
        is Duration -> value
        is String -> Duration.parse(value)
        else -> error("Unexpected value for Interval: $value of ${value::class}")
    }

    override fun notNullValueToDB(value: Duration): Any = value

    override fun nonNullValueToString(value: Duration): String = "'$value'"
}

abstract class YdbTypedStringColumnType(
    private val ydbSqlType: String,
    private val primitiveType: PrimitiveType
) : ColumnType<String>() {
    override fun sqlType(): String = ydbSqlType

    override fun valueFromDB(value: Any): String = value.toString()

    override fun notNullValueToDB(value: String): Any = value

    override fun nonNullValueToString(value: String): String =
        "'${value.replace("'", "''")}'"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        if (value == null) {
            super.setParameter(stmt, index, null)
            return
        }

        val ydbStatement = (stmt as? JdbcPreparedStatementImpl)?.statement as? YdbPreparedStatement
        if (ydbStatement != null) {
            ydbStatement.setObject(index, value, primitiveType)
        } else {
            super.setParameter(stmt, index, value)
        }
    }
}

class YdbJsonStringColumnType : YdbTypedStringColumnType("Json", PrimitiveType.Json)

class YdbJsonDocumentStringColumnType : YdbTypedStringColumnType("JsonDocument", PrimitiveType.JsonDocument)

/**
 * Maps a Kotlin [UUID] to the native YDB `Uuid` type.
 *
 * Binds [java.util.UUID] directly via JDBC — no string conversion. Use this for new schemas;
 * Exposed's built-in `uuid()` extension also produces a `Uuid` column under this dialect
 * because [YdbDataTypeProvider.uuidType] maps to `Uuid`.
 */
class YdbUuidColumnType : ColumnType<UUID>() {
    override fun sqlType(): String = "Uuid"

    override fun valueFromDB(value: Any): UUID = when (value) {
        is UUID -> value
        is String -> UUID.fromString(value)
        else -> error("Unexpected value for native UUID: $value of ${value::class}")
    }

    override fun notNullValueToDB(value: UUID): Any = value

    override fun nonNullValueToString(value: UUID): String = "Uuid(\"$value\")"
}

/**
 * Maps YDB `Uint64` to Kotlin [Long].
 *
 * Only the non-negative subset that fits into [Long] (0..[Long.MAX_VALUE]) is supported.
 * Use [BigInteger] mapping if you need values above [Long.MAX_VALUE].
 */
class YdbUint64ColumnType : ColumnType<Long>() {
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
}

private fun BigInteger.toLongCompatibleUint64(): Long {
    require(this >= BigInteger.ZERO) { "Uint64 value cannot be negative: $this" }
    require(this <= BigInteger.valueOf(Long.MAX_VALUE)) {
        "Uint64 value $this exceeds Long-backed Uint64 range (0..${Long.MAX_VALUE})"
    }
    return toLong()
}

fun Table.ydbDecimal(name: String, precision: Int, scale: Int): Column<BigDecimal> =
    registerColumn(name, YdbDecimalColumnType(precision, scale))

fun Table.ydbInterval(name: String): Column<Duration> =
    registerColumn(name, YdbIntervalColumnType())

fun Table.ydbJson(name: String): Column<String> =
    registerColumn(name, YdbJsonStringColumnType())

/** Indexed JSON storage — analogous to PostgreSQL `jsonb`. */
fun Table.ydbJsonDocument(name: String): Column<String> =
    registerColumn(name, YdbJsonDocumentStringColumnType())

/** Native YDB `Uuid` column. Equivalent to Exposed's `uuid()` under this dialect. */
fun Table.ydbUuid(name: String): Column<UUID> =
    registerColumn(name, YdbUuidColumnType())

fun Table.ydbUint64(name: String): Column<Long> =
    registerColumn(name, YdbUint64ColumnType())
