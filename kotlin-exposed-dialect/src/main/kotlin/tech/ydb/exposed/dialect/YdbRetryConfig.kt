package tech.ydb.exposed.dialect

/**
 * Retry and backoff settings for [ydbTransaction], aligned with
 * [YdbRetryPolicyConfig](https://github.com/ydb-platform/ydb-dotnet-sdk/blob/main/src/Ydb.Sdk/src/Ado/RetryPolicy/YdbRetryPolicyConfig.cs).
 */
data class YdbRetryConfig(
    /**
     * Total number of execution attempts (initial try + retries), same as .NET [maxAttempts].
     */
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    val fastBackoffBaseMs: Int = DEFAULT_FAST_BACKOFF_BASE_MS,
    val slowBackoffBaseMs: Int = DEFAULT_SLOW_BACKOFF_BASE_MS,
    val fastCapBackoffMs: Int = DEFAULT_FAST_CAP_BACKOFF_MS,
    val slowCapBackoffMs: Int = DEFAULT_SLOW_CAP_BACKOFF_MS,
    /**
     * When `true`, retryable statuses from [tech.ydb.exposed.dialect.YdbRetryPolicy] are retried
     * even if they are not classified as transient. Enable only for idempotent work.
     */
    val enableRetryIdempotence: Boolean = false,
) {
    internal val fastCeiling: Int
        get() = ceilingFromCapBackoffMs(fastCapBackoffMs)

    internal val slowCeiling: Int
        get() = ceilingFromCapBackoffMs(slowCapBackoffMs)

    companion object {
        const val DEFAULT_MAX_ATTEMPTS: Int = 10
        const val DEFAULT_FAST_BACKOFF_BASE_MS: Int = 5
        const val DEFAULT_SLOW_BACKOFF_BASE_MS: Int = 50
        const val DEFAULT_FAST_CAP_BACKOFF_MS: Int = 500
        const val DEFAULT_SLOW_CAP_BACKOFF_MS: Int = 5_000

        /** Default for read-write / non-idempotent transaction bodies. */
        @JvmField
        val DEFAULT: YdbRetryConfig = YdbRetryConfig(enableRetryIdempotence = false)

        /** Default for idempotent bodies (reads, single UPSERT/REPLACE, etc.). */
        @JvmField
        val IDEMPOTENT: YdbRetryConfig = YdbRetryConfig(enableRetryIdempotence = true)
    }
}

internal fun ceilingFromCapBackoffMs(capBackoffMs: Int): Int {
    val value = capBackoffMs + 1
    return kotlin.math.ceil(kotlin.math.ln(value.toDouble()) / kotlin.math.ln(2.0)).toInt()
}
