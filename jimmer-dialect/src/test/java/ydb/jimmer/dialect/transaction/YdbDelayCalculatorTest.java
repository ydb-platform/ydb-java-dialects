package ydb.jimmer.dialect.transaction;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YdbDelayCalculatorTest {
    @Test
    void shouldCalculateBackoffFromFirstRetryWithoutZeroDelayFormula() {
        RetryConfig config = RetryConfig.builder()
                .maxAttempts(5)
                .backoffMultiplier(100)
                .build();

        assertEquals(config.slowBackoffBaseMs(), YdbDelayCalculator.calculateBackoffMillis(
                config.slowBackoffBaseMs(),
                config.slowCapBackoffMs(),
                config.slowCeiling(),
                config.backoffMultiplier(),
                0
        ));
        assertEquals(config.fastBackoffBaseMs(), YdbDelayCalculator.calculateBackoffMillis(
                config.fastBackoffBaseMs(),
                config.fastCapBackoffMs(),
                config.fastCeiling(),
                config.backoffMultiplier(),
                0
        ));
    }

    @Test
    void shouldClampToCapBackoff() {
        RetryConfig config = RetryConfig.builder()
                .maxAttempts(5)
                .backoffBaseMs(1)
                .capBackoffMs(300)
                .build();

        assertEquals(config.slowCapBackoffMs(), YdbDelayCalculator.calculateBackoffMillis(
                config.slowBackoffBaseMs(),
                config.slowCapBackoffMs(),
                config.slowCeiling(),
                config.backoffMultiplier(),
                9
        ));
        assertEquals(config.fastCapBackoffMs(), YdbDelayCalculator.calculateBackoffMillis(
                config.fastBackoffBaseMs(),
                config.fastCapBackoffMs(),
                config.fastCeiling(),
                config.backoffMultiplier(),
                9
        ));
    }

    @Test
    void shouldUseInclusiveRangeForFullJitter() {
        Set<Long> observed = new HashSet<>();

        for (int i = 0; i < 256; i++) {
            long delay = YdbDelayCalculator.fullJitterMillis(1, 1, 1, 10, 0);
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
            long delay = YdbDelayCalculator.equalJitterMillis(2, 2, 1, 10, 0);
            assertTrue(delay >= 1 && delay <= 2);
            observed.add(delay);
        }

        assertTrue(observed.contains(1L));
        assertTrue(observed.contains(2L));
    }

    @Test
    void shouldKeepOddRemainderForFirstOverloadedRetry() {
        assertEquals(1, YdbDelayCalculator.equalJitterMillis(1, 1, 1, 2, 0));
    }
}
