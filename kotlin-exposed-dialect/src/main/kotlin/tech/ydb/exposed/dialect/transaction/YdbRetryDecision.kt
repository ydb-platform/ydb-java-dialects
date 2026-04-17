package tech.ydb.exposed.dialect.transaction

enum class YdbBackoffKind {
    NONE,
    INSTANT,
    FAST,
    SLOW
}

data class YdbRetryDecision(
    val retryable: Boolean,
    val recreateSession: Boolean = false,
    val backoffKind: YdbBackoffKind = YdbBackoffKind.NONE
)