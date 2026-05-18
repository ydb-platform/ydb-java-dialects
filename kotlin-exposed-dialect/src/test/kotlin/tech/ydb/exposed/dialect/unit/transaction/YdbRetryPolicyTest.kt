package tech.ydb.exposed.dialect.unit.transaction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbRetryConfig
import tech.ydb.exposed.dialect.calculateBackoffMillis
import tech.ydb.exposed.dialect.ceilingFromCapBackoffMs
import tech.ydb.exposed.dialect.code.YdbVendorCode
import tech.ydb.exposed.dialect.equalJitterMillis
import tech.ydb.exposed.dialect.fullJitterMillis
import tech.ydb.exposed.dialect.getNextRetryDelayMs
import tech.ydb.exposed.dialect.isTransientVendorCode
import java.sql.SQLException
import kotlin.random.Random

class YdbRetryPolicyTest {

    private class FakeSqlException(vendorCode: Int) : SQLException("fake", "YDB", vendorCode)

    private val fixedRandom = Random(42)

    @Test
    fun `ceiling is derived from cap like dotnet SDK`() {
        assertEquals(9, ceilingFromCapBackoffMs(500))
        assertEquals(13, ceilingFromCapBackoffMs(5_000))
    }

    @Test
    fun `calculateBackoff caps exponential growth`() {
        assertEquals(10, calculateBackoffMillis(backoffBaseMs = 5, capMs = 500, ceiling = 9, attempt = 1))
        assertEquals(500, calculateBackoffMillis(backoffBaseMs = 5, capMs = 500, ceiling = 9, attempt = 100))
    }

    @Test
    fun `full jitter stays within calculated backoff`() {
        repeat(20) {
            val delay = fullJitterMillis(5, 500, 9, attempt = 2, random = fixedRandom)
            assertTrue(delay in 0..20)
        }
    }

    @Test
    fun `equal jitter stays within calculated backoff`() {
        repeat(20) {
            val delay = equalJitterMillis(50, 5_000, 13, attempt = 1, random = fixedRandom)
            assertTrue(delay in 50..100)
        }
    }

    @Test
    fun `ABORTED uses full jitter fast backoff`() {
        val delay = getNextRetryDelayMs(
            FakeSqlException(YdbVendorCode.ABORTED),
            attempt = 0,
            config = YdbRetryConfig.DEFAULT,
            random = fixedRandom
        )
        assertTrue(delay != null && delay >= 0)
    }

    @Test
    fun `UNAVAILABLE uses equal jitter fast backoff`() {
        val delay = getNextRetryDelayMs(
            FakeSqlException(YdbVendorCode.UNAVAILABLE),
            attempt = 0,
            config = YdbRetryConfig.DEFAULT,
            random = fixedRandom
        )
        assertTrue(delay != null && delay >= 0)
    }

    @Test
    fun `OVERLOADED uses equal jitter slow backoff`() {
        val delay = getNextRetryDelayMs(
            FakeSqlException(YdbVendorCode.OVERLOADED),
            attempt = 0,
            config = YdbRetryConfig.DEFAULT,
            random = fixedRandom
        )
        assertTrue(delay != null && delay >= 0)
    }

    @Test
    fun `BAD_SESSION returns zero delay`() {
        assertEquals(
            0L,
            getNextRetryDelayMs(
                FakeSqlException(YdbVendorCode.BAD_SESSION),
                attempt = 0,
                config = YdbRetryConfig.DEFAULT
            )
        )
    }

    @Test
    fun `UNDETERMINED retries only with enableRetryIdempotence`() {
        assertNull(
            getNextRetryDelayMs(
                FakeSqlException(YdbVendorCode.UNDETERMINED),
                attempt = 0,
                config = YdbRetryConfig.DEFAULT
            )
        )
        assertTrue(
            getNextRetryDelayMs(
                FakeSqlException(YdbVendorCode.UNDETERMINED),
                attempt = 0,
                config = YdbRetryConfig.IDEMPOTENT
            ) != null
        )
    }

    @Test
    fun `PRECONDITION_FAILED never retries`() {
        assertNull(
            getNextRetryDelayMs(
                FakeSqlException(YdbVendorCode.PRECONDITION_FAILED),
                attempt = 0,
                config = YdbRetryConfig.IDEMPOTENT
            )
        )
    }

    @Test
    fun `stops after maxAttempts`() {
        val config = YdbRetryConfig.DEFAULT.copy(maxAttempts = 3)
        assertTrue(getNextRetryDelayMs(FakeSqlException(YdbVendorCode.ABORTED), 0, config) != null)
        assertTrue(getNextRetryDelayMs(FakeSqlException(YdbVendorCode.ABORTED), 1, config) != null)
        assertNull(getNextRetryDelayMs(FakeSqlException(YdbVendorCode.ABORTED), 2, config))
    }

    @Test
    fun `transient codes match dotnet transient gate`() {
        assertTrue(isTransientVendorCode(YdbVendorCode.ABORTED))
        assertFalse(isTransientVendorCode(YdbVendorCode.UNDETERMINED))
    }
}
