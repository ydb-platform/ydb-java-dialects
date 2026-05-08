package tech.ydb.retry;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.lang.Nullable;

public final class YdbRetryPolicyConfig {
    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_MAX_RETRIES = 10;
    public static final int DEFAULT_SLOW_BACKOFF_BASE_MS = 50;
    public static final int DEFAULT_FAST_BACKOFF_BASE_MS = 5;
    public static final int DEFAULT_SLOW_CAP_BACKOFF_MS = 5_000;
    public static final int DEFAULT_FAST_CAP_BACKOFF_MS = 500;

    private final boolean enabled;
    private final int maxRetries;
    private final int slowBackoffBaseMs;
    private final int fastBackoffBaseMs;
    private final int slowCapBackoffMs;
    private final int fastCapBackoffMs;
    private final int slowPow;
    private final int fastPow;

    public YdbRetryPolicyConfig() {
        this(
                DEFAULT_ENABLED,
                DEFAULT_MAX_RETRIES,
                DEFAULT_SLOW_BACKOFF_BASE_MS,
                DEFAULT_FAST_BACKOFF_BASE_MS,
                DEFAULT_SLOW_CAP_BACKOFF_MS,
                DEFAULT_FAST_CAP_BACKOFF_MS);
    }

    public YdbRetryPolicyConfig(
            boolean enabled,
            int maxRetries,
            int slowBackoffBaseMs,
            int fastBackoffBaseMs,
            int slowCapBackoffMs,
            int fastCapBackoffMs) {
        if (maxRetries < 1) {
            throw new IllegalArgumentException("maxRetries must be >= 1");
        }
        if (slowBackoffBaseMs < 0
                || fastBackoffBaseMs < 0
                || slowCapBackoffMs < 0
                || fastCapBackoffMs < 0) {
            throw new IllegalArgumentException("backoff values must be >= 0");
        }
        this.enabled = enabled;
        this.slowBackoffBaseMs = slowBackoffBaseMs;
        this.fastBackoffBaseMs = fastBackoffBaseMs;
        this.slowCapBackoffMs = slowCapBackoffMs;
        this.fastCapBackoffMs = fastCapBackoffMs;
        this.maxRetries = maxRetries;
        this.slowPow = powerForCap(this.slowCapBackoffMs);
        this.fastPow = powerForCap(this.fastCapBackoffMs);
    }

    public long getJitter(long bound) {
        if (bound <= 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextLong(bound + 1);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getSlowBackoffBaseMs() {
        return slowBackoffBaseMs;
    }

    public int getFastBackoffBaseMs() {
        return fastBackoffBaseMs;
    }

    public int getSlowCapBackoffMs() {
        return slowCapBackoffMs;
    }

    public int getFastCapBackoffMs() {
        return fastCapBackoffMs;
    }

    public int getSlowPow() {
        return slowPow;
    }

    public int getFastPow() {
        return fastPow;
    }

    public YdbRetryPolicyConfig merge(@Nullable YdbTransactional transactionPolicy) {
        if (transactionPolicy == null) {
            return this;
        }
        return new YdbRetryPolicyConfig(
                enabled && transactionPolicy.enabled(),
                mergeMaxRetries(transactionPolicy.maxRetries(), maxRetries),
                mergeNonNegativeInt(
                        "slowBackoffBaseMs", transactionPolicy.slowBackoffBaseMs(), slowBackoffBaseMs),
                mergeNonNegativeInt(
                        "fastBackoffBaseMs", transactionPolicy.fastBackoffBaseMs(), fastBackoffBaseMs),
                mergeNonNegativeInt(
                        "slowCapBackoffMs", transactionPolicy.slowCapBackoffMs(), slowCapBackoffMs),
                mergeNonNegativeInt(
                        "fastCapBackoffMs", transactionPolicy.fastCapBackoffMs(), fastCapBackoffMs));
    }

    private static int mergeMaxRetries(int candidate, int fallback) {
        return switch (candidate) {
            case -1 -> fallback;
            case 0 -> throw new IllegalArgumentException(
                    "maxRetries must not be 0; use enabled = false to disable retry");
            default -> {
                if (candidate < -1) {
                    throw new IllegalArgumentException("maxRetries must be -1 or >= 1");
                }
                yield candidate;
            }
        };
    }

    private static int mergeNonNegativeInt(String name, int candidate, int fallback)
            throws IllegalArgumentException {
        if (candidate < -1) {
            throw new IllegalArgumentException(String.format("%s is invalid", name));
        }
        return candidate == -1 ? fallback : candidate;
    }

    private static int powerForCap(int capMs) {
        if (capMs <= 0) {
            return 0;
        }
        return Integer.SIZE - Integer.numberOfLeadingZeros(capMs);
    }
}
