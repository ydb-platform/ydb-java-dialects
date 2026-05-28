package tech.ydb.retry;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Backoff math, ported one-to-one from {@code kotlin-exposed-dialect}'s {@code YdbRetryPolicy.kt}.
 *
 * <p>{@link #calculateBackoffMillis} computes the un-jittered backoff window as
 * {@code min(baseMs * 2^min(ceiling, attempt), capMs)}; {@link #fullJitterMillis} and
 * {@link #equalJitterMillis} then apply jitter on top of that window.
 */
public final class YdbDelayCalculator {

    private YdbDelayCalculator() {
    }

    /**
     * Pre-jitter backoff window: {@code min(baseMs * 2^min(ceiling, attempt), capMs)}.
     */
    public static int calculateBackoffMillis(int baseMs, int capMs, int ceiling, int attempt) {
        int shift = Math.min(ceiling, attempt);
        long scaled = (long) baseMs << shift;
        return (int) Math.min(scaled, capMs);
    }

    /** Full jitter: uniform in {@code [0, calculatedBackoff]}. */
    public static long fullJitterMillis(int baseMs, int capMs, int ceiling, int attempt) {
        int calculatedBackoff = calculateBackoffMillis(baseMs, capMs, ceiling, attempt);
        return randomLong(calculatedBackoff + 1L);
    }

    /**
     * Equal jitter: {@code calculatedBackoff/2 + calculatedBackoff%2 + random(0..calculatedBackoff/2)}.
     */
    public static long equalJitterMillis(int baseMs, int capMs, int ceiling, int attempt) {
        int calculatedBackoff = calculateBackoffMillis(baseMs, capMs, ceiling, attempt);
        int temp = calculatedBackoff / 2;
        return temp + calculatedBackoff % 2 + randomLong(temp + 1L);
    }

    private static long randomLong(long boundExclusive) {
        if (boundExclusive <= 0) {
            return 0L;
        }
        return ThreadLocalRandom.current().nextLong(boundExclusive);
    }
}
