package tech.ydb.exposed.dialect.unit.transaction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.core.Status
import tech.ydb.core.StatusCode
import tech.ydb.exposed.dialect.YdbBackoffKind
import tech.ydb.exposed.dialect.backoffMillis
import tech.ydb.exposed.dialect.classifyYdbError
import tech.ydb.jdbc.exception.YdbStatusable

class YdbRetryClassifierTest {

    private class FakeStatusException(code: StatusCode) :
        RuntimeException("fake"), YdbStatusable {
        private val s = Status.of(code)
        override fun getStatus(): Status = s
    }

    @Test
    fun `ABORTED is retryable with FAST backoff`() {
        val d = classifyYdbError(FakeStatusException(StatusCode.ABORTED), idempotent = false)
        assertTrue(d.retryable)
        assertEquals(YdbBackoffKind.FAST, d.backoffKind)
    }

    @Test
    fun `OVERLOADED is retryable with SLOW backoff`() {
        val d = classifyYdbError(FakeStatusException(StatusCode.OVERLOADED), idempotent = false)
        assertTrue(d.retryable)
        assertEquals(YdbBackoffKind.SLOW, d.backoffKind)
    }

    @Test
    fun `BAD_SESSION is retryable with INSTANT backoff`() {
        val d = classifyYdbError(FakeStatusException(StatusCode.BAD_SESSION), idempotent = false)
        assertTrue(d.retryable)
        assertEquals(YdbBackoffKind.INSTANT, d.backoffKind)
    }

    @Test
    fun `PRECONDITION_FAILED is not retryable`() {
        val d = classifyYdbError(FakeStatusException(StatusCode.PRECONDITION_FAILED), idempotent = true)
        assertFalse(d.retryable)
    }

    @Test
    fun `TIMEOUT retries only when idempotent`() {
        assertTrue(classifyYdbError(FakeStatusException(StatusCode.TIMEOUT), idempotent = true).retryable)
        assertFalse(classifyYdbError(FakeStatusException(StatusCode.TIMEOUT), idempotent = false).retryable)
    }

    @Test
    fun `UNDETERMINED retries only when idempotent`() {
        assertTrue(classifyYdbError(FakeStatusException(StatusCode.UNDETERMINED), idempotent = true).retryable)
        assertFalse(classifyYdbError(FakeStatusException(StatusCode.UNDETERMINED), idempotent = false).retryable)
    }

    @Test
    fun `text-only error without YdbStatusable is treated as non-retryable`() {
        val d = classifyYdbError(RuntimeException("Status{code = ABORTED}"), idempotent = true)
        assertFalse(d.retryable)
    }

    @Test
    fun `walks cause chain to find a YdbStatusable`() {
        val cause = FakeStatusException(StatusCode.ABORTED)
        val wrapped = RuntimeException("outer", RuntimeException("middle", cause))

        val d = classifyYdbError(wrapped, idempotent = false)
        assertTrue(d.retryable)
        assertEquals(YdbBackoffKind.FAST, d.backoffKind)
    }

    @Test
    fun `backoffMillis returns non-negative values`() {
        assertTrue(backoffMillis(YdbBackoffKind.FAST, 1) >= 0)
        assertTrue(backoffMillis(YdbBackoffKind.SLOW, 1) >= 0)
        assertEquals(0L, backoffMillis(YdbBackoffKind.NONE, 1))
        assertEquals(0L, backoffMillis(YdbBackoffKind.INSTANT, 1))
    }
}
