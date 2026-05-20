package tech.ydb.exposed.dialect

import tech.ydb.exposed.dialect.code.YdbVendorCode
import java.sql.SQLException
import kotlin.math.min
import kotlin.random.Random

/**
 * Retry delay calculation for [ydbTransaction] from JDBC [SQLException.errorCode] (YDB vendor codes).
 *
 * - [fullJitterMillis] — `Aborted`, `Undetermined`
 * - [equalJitterMillis] — `Unavailable`, transport errors, `Overloaded`, resource exhausted
 * - zero delay — `BadSession`, `SessionBusy`, `SessionExpired`
 *
 * Not retried here (add handling if your workload needs it): `TIMEOUT`, `CLIENT_DEADLINE_EXPIRED`,
 * `PRECONDITION_FAILED`, and other vendor codes. [YdbRetryConfig.enableRetryIdempotence] retries
 * any code returned by this policy when `true`, not only [isTransientVendorCode].
 * `SESSION_EXPIRED` is retried with zero backoff but is not in the transient set.
 */
internal fun getNextRetryDelayMs(
    error: Throwable,
    attempt: Int,
    config: YdbRetryConfig,
    random: Random = Random.Default
): Long? {
    if (attempt >= config.maxAttempts - 1) {
        return null
    }

    val vendorCode = extractVendorCode(error) ?: return null

    if (!config.enableRetryIdempotence && !isTransientVendorCode(vendorCode)) {
        return null
    }

    return when (vendorCode) {
        YdbVendorCode.BAD_SESSION,
        YdbVendorCode.SESSION_BUSY,
        YdbVendorCode.SESSION_EXPIRED -> 0L

        YdbVendorCode.ABORTED,
        YdbVendorCode.UNDETERMINED ->
            fullJitterMillis(
                backoffBaseMs = config.fastBackoffBaseMs,
                capMs = config.fastCapBackoffMs,
                ceiling = config.fastCeiling,
                attempt = attempt,
                random = random
            )

        YdbVendorCode.UNAVAILABLE,
        YdbVendorCode.TRANSPORT_UNAVAILABLE,
        YdbVendorCode.CLIENT_GRPC_ERROR ->
            equalJitterMillis(
                backoffBaseMs = config.fastBackoffBaseMs,
                capMs = config.fastCapBackoffMs,
                ceiling = config.fastCeiling,
                attempt = attempt,
                random = random
            )

        YdbVendorCode.OVERLOADED,
        YdbVendorCode.CLIENT_RESOURCE_EXHAUSTED ->
            equalJitterMillis(
                backoffBaseMs = config.slowBackoffBaseMs,
                capMs = config.slowCapBackoffMs,
                ceiling = config.slowCeiling,
                attempt = attempt,
                random = random
            )

        else -> null
    }
}

/** Vendor codes retried without [YdbRetryConfig.enableRetryIdempotence]. */
internal fun isTransientVendorCode(vendorCode: Int): Boolean = vendorCode in TRANSIENT_VENDOR_CODES

internal fun extractVendorCode(error: Throwable): Int? {
    var current: Throwable? = error
    while (current != null) {
        if (current is SQLException) {
            val vendorCode = current.errorCode
            if (vendorCode != 0) {
                return vendorCode
            }
        }
        current = current.cause
    }
    return null
}

internal fun calculateBackoffMillis(
    backoffBaseMs: Int,
    capMs: Int,
    ceiling: Int,
    attempt: Int
): Int = min(backoffBaseMs * (1 shl min(ceiling, attempt)), capMs)

/** Full jitter: uniform in `[0, calculatedBackoff]`. */
internal fun fullJitterMillis(
    backoffBaseMs: Int,
    capMs: Int,
    ceiling: Int,
    attempt: Int,
    random: Random
): Long {
    val calculatedBackoff = calculateBackoffMillis(backoffBaseMs, capMs, ceiling, attempt)
    return random.nextLong(calculatedBackoff + 1L)
}

/** Equal jitter: `calculatedBackoff/2 + calculatedBackoff%2 + random(0..calculatedBackoff/2)`. */
internal fun equalJitterMillis(
    backoffBaseMs: Int,
    capMs: Int,
    ceiling: Int,
    attempt: Int,
    random: Random
): Long {
    val calculatedBackoff = calculateBackoffMillis(backoffBaseMs, capMs, ceiling, attempt)
    val temp = calculatedBackoff / 2
    return temp + calculatedBackoff % 2 + random.nextLong(temp + 1L)
}

private val TRANSIENT_VENDOR_CODES: Set<Int> = setOf(
    YdbVendorCode.ABORTED,
    YdbVendorCode.UNAVAILABLE,
    YdbVendorCode.OVERLOADED,
    YdbVendorCode.CLIENT_RESOURCE_EXHAUSTED,
    YdbVendorCode.BAD_SESSION,
    YdbVendorCode.SESSION_BUSY
)
