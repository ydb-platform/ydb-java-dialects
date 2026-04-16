package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.core.Column

enum class YdbTtlColumnMode {
    DATE_TYPE,
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

data class YdbTtlSettings(
    val column: Column<*>,
    val intervalIso8601: String,
    val mode: YdbTtlColumnMode = YdbTtlColumnMode.DATE_TYPE
)