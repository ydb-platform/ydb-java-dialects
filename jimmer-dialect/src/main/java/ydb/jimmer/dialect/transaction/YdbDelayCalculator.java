package ydb.jimmer.dialect.transaction;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Backoff math, ported from {@code spring-ydb-retry}'s {@code YdbRetryPolicy.java}.
 * Added support for changing the multiplier for the backoff/delay.
 *
 * <p>{@link #calculateBackoffMillis} computes the un-jittered backoff window as
 * {@code min(baseMs * 2^min(ceiling, attempt), capMs)}; {@link #fullJitterMillis} and
 * {@link #equalJitterMillis} then apply jitter on top of that window.
 */
public final class YdbDelayCalculator {
    private YdbDelayCalculator() {}

    /**
     * Pre-jitter backoff window: {@code min(baseMs * multiplier^min(ceiling, attempt), capMs)}.
     */
    public static int calculateBackoffMillis(int baseMs, int capMs, int ceiling, double multiplier, int attempt) {
        int n = Math.min(ceiling, attempt);
        long scaled = (long) ((long) baseMs * Math.pow(multiplier, n));
        return (int) Math.min(scaled, capMs);
    }

    /**
     * Full jitter: uniform in {@code [0, calculatedBackoff]}.
     */
    public static long fullJitterMillis(int baseMs, int capMs, int ceiling, double multiplier, int attempt) {
        int calculatedBackoff = calculateBackoffMillis(baseMs, capMs, ceiling, multiplier, attempt);
        return randomLong(calculatedBackoff + 1L);
    }

    /**
     * Equal jitter: {@code calculatedBackoff/2 + calculatedBackoff%2 + random(0..calculatedBackoff/2)}.
     */
    public static long equalJitterMillis(int baseMs, int capMs, int ceiling, double multiplier, int attempt) {
        int calculatedBackoff = calculateBackoffMillis(baseMs, capMs, ceiling, multiplier, attempt);
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
