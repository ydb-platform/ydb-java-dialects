/**
 * TTL settings for [YdbTable.ttl] — see YDB docs on `WITH (TTL = Interval(...) ON ...)`.
 */
package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Column

/** How the TTL source [Column] value is interpreted in YQL. */
enum class YdbTtlColumnMode {
    /** `Date` / `Datetime` / `Timestamp` (and `*32`/`*64` variants) columns. */
    DATE_TYPE,
    /** `Uint32` / `Uint64` / `DyNumber` interpreted as Unix seconds. */
    SECONDS,
    MILLISECONDS,
    MICROSECONDS,
    NANOSECONDS;

    fun toSql(): String? = when (this) {
        DATE_TYPE -> null
        SECONDS -> "SECONDS"
        MILLISECONDS -> "MILLISECONDS"
        MICROSECONDS -> "MICROSECONDS"
        NANOSECONDS -> "NANOSECONDS"
    }
}

/** Resolved TTL clause for [YdbTable] DDL generation. */
data class YdbTtlSettings(
    /** Column whose value drives expiration. */
    val column: Column<*>,
    /** ISO-8601 duration string passed to `Interval("...")`. */
    val intervalIso8601: String,
    val mode: YdbTtlColumnMode = YdbTtlColumnMode.DATE_TYPE
)