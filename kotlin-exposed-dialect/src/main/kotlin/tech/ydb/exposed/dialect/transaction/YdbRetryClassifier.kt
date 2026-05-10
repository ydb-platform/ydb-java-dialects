package tech.ydb.exposed.dialect.transaction

import tech.ydb.core.StatusCode
import tech.ydb.jdbc.exception.YdbStatusable
import kotlin.math.min
import kotlin.random.Random

object YdbRetryClassifier {
    private val statusCodeRegex = Regex("""STATUS\{CODE\s*=\s*([A-Z_]+)""")

    fun classify(error: Throwable, idempotent: Boolean): YdbRetryDecision {
        val code = extractStatusCode(error)

        return when (code) {
            StatusCode.ABORTED ->
                YdbRetryDecision(retryable = true, backoffKind = YdbBackoffKind.FAST)

            StatusCode.UNAVAILABLE,
            StatusCode.TRANSPORT_UNAVAILABLE,
            StatusCode.CLIENT_DISCOVERY_FAILED,
            StatusCode.CLIENT_GRPC_ERROR,
            StatusCode.CLIENT_INTERNAL_ERROR ->
                YdbRetryDecision(retryable = true, backoffKind = YdbBackoffKind.FAST)

            StatusCode.OVERLOADED,
            StatusCode.CLIENT_RESOURCE_EXHAUSTED,
            StatusCode.CLIENT_LIMITS_REACHED ->
                YdbRetryDecision(retryable = true, backoffKind = YdbBackoffKind.SLOW)

            StatusCode.BAD_SESSION,
            StatusCode.SESSION_EXPIRED ->
                YdbRetryDecision(retryable = true, recreateSession = true, backoffKind = YdbBackoffKind.INSTANT)

            StatusCode.SESSION_BUSY ->
                YdbRetryDecision(retryable = true, recreateSession = true, backoffKind = YdbBackoffKind.FAST)

            StatusCode.TIMEOUT,
            StatusCode.CLIENT_DEADLINE_EXCEEDED,
            StatusCode.CLIENT_DEADLINE_EXPIRED ->
                YdbRetryDecision(retryable = idempotent, backoffKind = if (idempotent) YdbBackoffKind.INSTANT else YdbBackoffKind.NONE)

            StatusCode.UNDETERMINED ->
                YdbRetryDecision(retryable = idempotent, backoffKind = if (idempotent) YdbBackoffKind.FAST else YdbBackoffKind.NONE)

            StatusCode.PRECONDITION_FAILED,
            StatusCode.ALREADY_EXISTS,
            StatusCode.NOT_FOUND,
            StatusCode.SCHEME_ERROR,
            StatusCode.GENERIC_ERROR,
            StatusCode.INTERNAL_ERROR,
            StatusCode.BAD_REQUEST,
            StatusCode.UNAUTHORIZED,
            StatusCode.UNSUPPORTED,
            StatusCode.CANCELLED,
            StatusCode.CLIENT_CANCELLED,
            StatusCode.CLIENT_UNAUTHENTICATED,
            StatusCode.CLIENT_CALL_UNIMPLEMENTED,
            StatusCode.EXTERNAL_ERROR,
            StatusCode.UNUSED_STATUS,
            StatusCode.SUCCESS,
            null ->
                YdbRetryDecision(retryable = false)
        }
    }

    private fun extractStatusCode(error: Throwable): StatusCode? {
        var current: Throwable? = error
        while (current != null) {
            if (current is YdbStatusable) {
                return current.status.code
            }

            val message = current.message.orEmpty().uppercase()
            val matched = statusCodeRegex.find(message)?.groupValues?.getOrNull(1)
            if (matched != null) {
                runCatching { return StatusCode.valueOf(matched) }
            }

            current = current.cause
        }

        return null
    }

    fun backoffMillis(kind: YdbBackoffKind, attempt: Int): Long {
        val n = attempt.coerceAtLeast(1)
        return when (kind) {
            YdbBackoffKind.NONE -> 0L
            YdbBackoffKind.INSTANT -> 0L
            YdbBackoffKind.FAST -> {
                val base = 25L * (1L shl min(n - 1, 5))
                jitter(base, 15)
            }
            YdbBackoffKind.SLOW -> {
                val base = 200L * (1L shl min(n - 1, 4))
                jitter(base, 50)
            }
        }
    }

    private fun jitter(base: Long, spreadPercent: Int): Long {
        if (base <= 0) return 0L
        val spread = (base * spreadPercent) / 100
        return base + Random.nextLong(-spread, spread + 1)
    }
}
