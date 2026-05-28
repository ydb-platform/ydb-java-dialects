package tech.ydb.retry;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YdbDelayCalculatorTest {

    @Test
    void shouldCalculateBackoffFromFirstRetryWithoutZeroDelayFormula() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 5, 50, 5, 5_000, 500);

        assertEquals(50, YdbDelayCalculator.calculateBackoffMillis(50, 5_000, config.getSlowCeiling(), 0));
        assertEquals(5, YdbDelayCalculator.calculateBackoffMillis(5, 500, config.getFastCeiling(), 0));
    }

    @Test
    void shouldClampToCapBackoff() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 5, 1, 1, 300, 300);

        assertEquals(300,
                YdbDelayCalculator.calculateBackoffMillis(1, 300, config.getSlowCeiling(), 9));
        assertEquals(300,
                YdbDelayCalculator.calculateBackoffMillis(1, 300, config.getFastCeiling(), 9));
    }

    @Test
    void shouldUseInclusiveRangeForFullJitter() {
        Set<Long> observed = new HashSet<>();

        for (int i = 0; i < 256; i++) {
            long delay = YdbDelayCalculator.fullJitterMillis(1, 1, 1, 0);
            assertTrue(delay >= 0 && delay <= 1);
            observed.add(delay);
        }

        assertTrue(observed.contains(0L));
        assertTrue(observed.contains(1L));
    }

    @Test
    void shouldUseEqualJitterRange() {
        Set<Long> observed = new HashSet<>();

        for (int i = 0; i < 256; i++) {
            long delay = YdbDelayCalculator.equalJitterMillis(2, 2, 1, 0);
            assertTrue(delay >= 1 && delay <= 2);
            observed.add(delay);
        }

        assertTrue(observed.contains(1L));
        assertTrue(observed.contains(2L));
    }

    @Test
    void shouldKeepOddRemainderForFirstOverloadedRetry() {
        assertEquals(1, YdbDelayCalculator.equalJitterMillis(1, 1, 1, 0));
    }
}
