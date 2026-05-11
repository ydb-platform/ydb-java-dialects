package tech.ydb.retry;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.ydb.core.StatusCode.ABORTED;
import static tech.ydb.core.StatusCode.OVERLOADED;
import static tech.ydb.core.StatusCode.UNAVAILABLE;

class YdbDelayCalculatorTest {

    @Test
    void shouldCalculateBackoffFromFirstRetryWithoutZeroDelayFormula() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 5, 50, 5, 5_000, 500);

        assertEquals(50, YdbDelayCalculator.calculateBackoff(50, 5_000, config.getSlowPow(), 0));
        assertEquals(5, YdbDelayCalculator.calculateBackoff(5, 500, config.getFastPow(), 0));
    }

    @Test
    void shouldUseDotNetCeilingWhenCalculatingBackoffCap() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 5, 1, 1, 300, 300);

        assertEquals(300, YdbDelayCalculator.calculateBackoff(1, 300, config.getSlowPow(), 9));
        assertEquals(300, YdbDelayCalculator.calculateBackoff(1, 300, config.getFastPow(), 9));
    }

    @Test
    void shouldUseInclusiveRangeForFullJitter() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 5, 1, 1, 1, 1);
        Set<Long> observed = new HashSet<>();

        for (int i = 0; i < 256; i++) {
            long delay = YdbDelayCalculator.calculateDelay(ABORTED, config, 0);
            assertTrue(delay >= 0 && delay <= 1);
            observed.add(delay);
        }

        assertTrue(observed.contains(0L));
        assertTrue(observed.contains(1L));
    }

    @Test
    void shouldUseEqualJitterRange() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 5, 2, 2, 2, 2);
        Set<Long> observed = new HashSet<>();

        for (int i = 0; i < 256; i++) {
            long delay = YdbDelayCalculator.calculateDelay(UNAVAILABLE, config, 0);
            assertTrue(delay >= 1 && delay <= 2);
            observed.add(delay);
        }

        assertTrue(observed.contains(1L));
        assertTrue(observed.contains(2L));
    }

    @Test
    void shouldKeepOddRemainderForFirstOverloadedRetry() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 5, 1, 1, 1, 1);

        assertEquals(1, YdbDelayCalculator.calculateDelay(OVERLOADED, config, 0));
    }
}
