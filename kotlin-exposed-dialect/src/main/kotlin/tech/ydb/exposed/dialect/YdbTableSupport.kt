package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.time.Duration

internal fun buildYdbCreateStatement(
    table: Table,
    ttlSettings: YdbTtlSettings?,
    secondaryIndices: List<YdbSecondaryIndexSpec>
): List<String> {
    val tr = TransactionManager.current()

    val pk = table.primaryKey
        ?: error("YDB requires PRIMARY KEY for every table: ${table.tableName}")

    val columnsSql = table.columns.joinToString(", ") { column ->
        buildString {
            append(tr.identity(column))
            append(" ")
            append(column.columnType.sqlType())

            if (!column.columnType.nullable) {
                append(" NOT NULL")
            }
        }
    }

    val indexesSql = secondaryIndices.joinToString(", ") { renderYdbSecondaryIndex(it) }
    val pkSql = pk.columns.joinToString(", ") { tr.identity(it) }

    val ttlSql = ttlSettings?.let { ttl ->
        validateYdbTtlColumn(ttl)
        val normalizedInterval = normalizeTtlInterval(ttl.intervalIso8601)

        buildString {
            append(" WITH (TTL = Interval(\"")
            append(escapeYqlDoubleQuotedLiteral(normalizedInterval))
            append("\") ON ")
            append(tr.identity(ttl.column))
            ttl.mode.toSql()?.let {
                append(" AS ")
                append(it)
            }
            append(")")
        }
    }.orEmpty()

    val sql = buildString {
        append("CREATE TABLE IF NOT EXISTS ")
        append(tr.identity(table))
        append(" (")
        append(columnsSql)

        if (indexesSql.isNotEmpty()) {
            append(", ")
            append(indexesSql)
        }

        append(", PRIMARY KEY (")
        append(pkSql)
        append("))")
        append(ttlSql)
    }

    return listOf(sql)
}

internal fun normalizeTtlInterval(intervalIso8601: String): String =
    runCatching { Duration.parse(intervalIso8601).toString() }
        .getOrElse { cause ->
            throw IllegalArgumentException("Invalid YDB TTL interval: '$intervalIso8601'", cause)
        }

internal fun validateYdbTtlColumn(ttl: YdbTtlSettings) {
    val sqlType = ttl.column.columnType.sqlType()

    val supported = when (ttl.mode) {
        YdbTtlColumnMode.DATE_TYPE ->
            sqlType == "Date" ||
                sqlType == "Date32" ||
                sqlType == "Datetime" ||
                sqlType == "Datetime64" ||
                sqlType == "Timestamp" ||
                sqlType == "Timestamp64"

        YdbTtlColumnMode.SECONDS,
        YdbTtlColumnMode.MILLISECONDS,
        YdbTtlColumnMode.MICROSECONDS,
        YdbTtlColumnMode.NANOSECONDS ->
            sqlType == "Uint32" || sqlType == "Uint64" || sqlType == "DyNumber"
    }

    require(supported) {
        "YDB TTL does not support column '${ttl.column.name}' of type '$sqlType' for mode '${ttl.mode}'"
    }
}
