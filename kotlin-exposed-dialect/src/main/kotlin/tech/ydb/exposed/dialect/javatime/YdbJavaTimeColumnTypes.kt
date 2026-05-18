@file:OptIn(kotlin.time.ExperimentalTime::class)

package tech.ydb.exposed.dialect.javatime

import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.datetime.InstantColumnType
import org.jetbrains.exposed.v1.core.datetime.LocalDateColumnType
import org.jetbrains.exposed.v1.core.datetime.LocalDateTimeColumnType
import org.jetbrains.exposed.v1.core.statements.api.PreparedStatementApi
import tech.ydb.exposed.dialect.bindYdbParameter
import tech.ydb.exposed.dialect.code.YdbJdbcCode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

// region Table column extensions

/** Legacy unsigned YDB `Date` ([LocalDate]). */
fun Table.ydbDate(name: String): Column<LocalDate> =
    registerColumn(name, YdbDateColumnType(YdbJdbcCode.DATE))

/** Extended signed YDB `Date32` ([LocalDate]). */
fun Table.ydbDate32(name: String): Column<LocalDate> =
    registerColumn(name, YdbDateColumnType(YdbJdbcCode.DATE32))

/** Legacy unsigned YDB `Datetime` ([LocalDateTime]). */
fun Table.ydbDatetime(name: String): Column<LocalDateTime> =
    registerColumn(name, YdbDateTimeColumnType(YdbJdbcCode.DATETIME))

/** Extended signed YDB `Datetime64` ([LocalDateTime]). */
fun Table.ydbDatetime64(name: String): Column<LocalDateTime> =
    registerColumn(name, YdbDateTimeColumnType(YdbJdbcCode.DATETIME64))

/** Legacy unsigned YDB `Timestamp` ([Instant]). */
fun Table.ydbTimestamp(name: String): Column<Instant> =
    registerColumn(name, YdbTimestampColumnType(YdbJdbcCode.TIMESTAMP))

/** Extended signed YDB `Timestamp64` ([Instant]). */
fun Table.ydbTimestamp64(name: String): Column<Instant> =
    registerColumn(name, YdbTimestampColumnType(YdbJdbcCode.TIMESTAMP64))

// endregion

// region Column types

internal class YdbDateColumnType(
    private val jdbcTypeCode: Int
) : LocalDateColumnType<LocalDate>() {
    override fun sqlType(): String =
        if (jdbcTypeCode == YdbJdbcCode.DATE) "Date" else "Date32"

    override fun toLocalDate(value: LocalDate): kotlinx.datetime.LocalDate = value.toKotlinLocalDate()

    override fun fromLocalDate(value: kotlinx.datetime.LocalDate): LocalDate = value.toJavaLocalDate()

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        bindYdbParameter(stmt, index, value, jdbcTypeCode, this)
    }
}

internal class YdbDateTimeColumnType(
    private val jdbcTypeCode: Int
) : LocalDateTimeColumnType<LocalDateTime>() {
    override fun sqlType(): String =
        if (jdbcTypeCode == YdbJdbcCode.DATETIME) "Datetime" else "Datetime64"

    override fun toLocalDateTime(value: LocalDateTime): kotlinx.datetime.LocalDateTime =
        value.toKotlinLocalDateTime()

    override fun fromLocalDateTime(value: kotlinx.datetime.LocalDateTime): LocalDateTime =
        value.toJavaLocalDateTime()

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        bindYdbParameter(stmt, index, value, jdbcTypeCode, this)
    }
}

internal class YdbTimestampColumnType(
    private val jdbcTypeCode: Int
) : InstantColumnType<Instant>() {
    override fun sqlType(): String =
        if (jdbcTypeCode == YdbJdbcCode.TIMESTAMP) "Timestamp" else "Timestamp64"

    override fun toInstant(value: Instant): kotlin.time.Instant =
        kotlin.time.Instant.fromEpochMilliseconds(value.toEpochMilli())

    override fun fromInstant(value: kotlin.time.Instant): Instant =
        Instant.ofEpochMilli(value.toEpochMilliseconds())

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        bindYdbParameter(stmt, index, value, jdbcTypeCode, this)
    }
}

// endregion
