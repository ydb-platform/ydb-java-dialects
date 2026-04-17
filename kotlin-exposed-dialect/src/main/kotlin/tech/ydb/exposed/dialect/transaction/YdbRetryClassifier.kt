package tech.ydb.exposed.dialect.transaction

import java.sql.SQLException
import kotlin.math.min
import kotlin.random.Random

object YdbRetryClassifier {

    fun classify(error: Throwable, idempotent: Boolean): YdbRetryDecision {
        val text = buildString {
            append(error.message.orEmpty())
            val causeMessage = error.cause?.message.orEmpty()
            if (causeMessage.isNotBlank()) {
                append(" | ")
                append(causeMessage)
            }
        }.uppercase()

        return when {
            "ABORTED" in text ->
                YdbRetryDecision(retryable = true, backoffKind = YdbBackoffKind.FAST)

            "UNAVAILABLE" in text ->
                YdbRetryDecision(retryable = true, backoffKind = YdbBackoffKind.FAST)

            "OVERLOADED" in text ->
                YdbRetryDecision(retryable = true, backoffKind = YdbBackoffKind.SLOW)

            "BAD_SESSION" in text ->
                YdbRetryDecision(retryable = true, recreateSession = true, backoffKind = YdbBackoffKind.INSTANT)

            "SESSION_EXPIRED" in text ->
                YdbRetryDecision(retryable = true, recreateSession = true, backoffKind = YdbBackoffKind.INSTANT)

            "SESSION_BUSY" in text ->
                YdbRetryDecision(retryable = true, recreateSession = true, backoffKind = YdbBackoffKind.FAST)

            "TIMEOUT" in text ->
                YdbRetryDecision(retryable = idempotent, backoffKind = if (idempotent) YdbBackoffKind.INSTANT else YdbBackoffKind.NONE)

            "UNDETERMINED" in text ->
                YdbRetryDecision(retryable = idempotent, backoffKind = if (idempotent) YdbBackoffKind.FAST else YdbBackoffKind.NONE)

            "PRECONDITION_FAILED" in text ->
                YdbRetryDecision(retryable = false)

            "ALREADY_EXISTS" in text ->
                YdbRetryDecision(retryable = false)

            "NOT_FOUND" in text ->
                YdbRetryDecision(retryable = false)

            "SCHEME_ERROR" in text ->
                YdbRetryDecision(retryable = false)

            "GENERIC_ERROR" in text ->
                YdbRetryDecision(retryable = false)

            else ->
                YdbRetryDecision(retryable = false)
        }
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