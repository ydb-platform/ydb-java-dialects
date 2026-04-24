package tech.ydb.exposed.dialect.unit.transaction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.transaction.YdbBackoffKind
import tech.ydb.exposed.dialect.transaction.YdbRetryClassifier

class YdbRetryClassifierTest {

    @Test
    fun `should classify aborted as retryable fast`() {
        val decision = YdbRetryClassifier.classify(
            RuntimeException("Status{code = ABORTED(code=400040)}"),
            idempotent = false
        )

        assertTrue(decision.retryable)
        assertEquals(YdbBackoffKind.FAST, decision.backoffKind)
        assertFalse(decision.recreateSession)
    }

    @Test
    fun `should classify overloaded as retryable slow`() {
        val decision = YdbRetryClassifier.classify(
            RuntimeException("Status{code = OVERLOADED(code=400060)}"),
            idempotent = false
        )

        assertTrue(decision.retryable)
        assertEquals(YdbBackoffKind.SLOW, decision.backoffKind)
    }

    @Test
    fun `should classify bad session as retryable with recreate`() {
        val decision = YdbRetryClassifier.classify(
            RuntimeException("Status{code = BAD_SESSION(code=400100)}"),
            idempotent = false
        )

        assertTrue(decision.retryable)
        assertTrue(decision.recreateSession)
        assertEquals(YdbBackoffKind.INSTANT, decision.backoffKind)
    }

    @Test
    fun `should classify precondition failed as non retryable`() {
        val decision = YdbRetryClassifier.classify(
            RuntimeException("Status{code = PRECONDITION_FAILED(code=400120)}"),
            idempotent = false
        )

        assertFalse(decision.retryable)
    }

    @Test
    fun `should classify timeout as retryable only for idempotent operations`() {
        val retryable = YdbRetryClassifier.classify(
            RuntimeException("Status{code = TIMEOUT(code=400090)}"),
            idempotent = true
        )
        val nonRetryable = YdbRetryClassifier.classify(
            RuntimeException("Status{code = TIMEOUT(code=400090)}"),
            idempotent = false
        )

        assertTrue(retryable.retryable)
        assertFalse(nonRetryable.retryable)
    }

    @Test
    fun `should classify undetermined as retryable only for idempotent operations`() {
        val retryable = YdbRetryClassifier.classify(
            RuntimeException("Status{code = UNDETERMINED(code=400170)}"),
            idempotent = true
        )
        val nonRetryable = YdbRetryClassifier.classify(
            RuntimeException("Status{code = UNDETERMINED(code=400170)}"),
            idempotent = false
        )

        assertTrue(retryable.retryable)
        assertFalse(nonRetryable.retryable)
    }

    @Test
    fun `should produce backoff for fast and slow kinds`() {
        val fast = YdbRetryClassifier.backoffMillis(YdbBackoffKind.FAST, 1)
        val slow = YdbRetryClassifier.backoffMillis(YdbBackoffKind.SLOW, 1)

        assertTrue(fast >= 0)
        assertTrue(slow >= 0)
    }
}