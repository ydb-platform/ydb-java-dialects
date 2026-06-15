package tech.ydb.retry;

import org.springframework.lang.Nullable;

/**
 * Retry and backoff settings.
 *
 * <p>Two-tier backoff matches {@code kotlin-exposed-dialect}'s {@code YdbRetryConfig}:
 * <ul>
 *   <li><b>Fast tier:</b> {@code ABORTED}, {@code UNDETERMINED}, {@code UNAVAILABLE},
 *       transport errors.</li>
 *   <li><b>Slow tier:</b> {@code OVERLOADED}, {@code CLIENT_RESOURCE_EXHAUSTED}.</li>
 * </ul>
 */
public final class YdbRetryPolicyConfig {
    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_MAX_ATTEMPTS = 10;
    public static final int DEFAULT_SLOW_BACKOFF_BASE_MS = 50;
    public static final int DEFAULT_FAST_BACKOFF_BASE_MS = 5;
    public static final int DEFAULT_SLOW_CAP_BACKOFF_MS = 5_000;
    public static final int DEFAULT_FAST_CAP_BACKOFF_MS = 500;

    private final boolean enabled;
    private final int maxAttempts;
    private final int slowBackoffBaseMs;
    private final int fastBackoffBaseMs;
    private final int slowCapBackoffMs;
    private final int fastCapBackoffMs;
    private final int slowCeiling;
    private final int fastCeiling;

    public YdbRetryPolicyConfig() {
        this(
                DEFAULT_ENABLED,
                DEFAULT_MAX_ATTEMPTS,
                DEFAULT_SLOW_BACKOFF_BASE_MS,
                DEFAULT_FAST_BACKOFF_BASE_MS,
                DEFAULT_SLOW_CAP_BACKOFF_MS,
                DEFAULT_FAST_CAP_BACKOFF_MS);
    }

    public YdbRetryPolicyConfig(
            boolean enabled,
            int maxAttempts,
            int slowBackoffBaseMs,
            int fastBackoffBaseMs,
            int slowCapBackoffMs,
            int fastCapBackoffMs) {
        if (maxAttempts < 0) {
            throw new IllegalArgumentException("maxAttempts must be >= 0");
        }
        if (slowBackoffBaseMs < 0
                || fastBackoffBaseMs < 0
                || slowCapBackoffMs < 0
                || fastCapBackoffMs < 0) {
            throw new IllegalArgumentException("backoff values must be >= 0");
        }
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;
        this.slowBackoffBaseMs = slowBackoffBaseMs;
        this.fastBackoffBaseMs = fastBackoffBaseMs;
        this.slowCapBackoffMs = slowCapBackoffMs;
        this.fastCapBackoffMs = fastCapBackoffMs;
        this.slowCeiling = ceilingFromCapBackoffMs(slowCapBackoffMs);
        this.fastCeiling = ceilingFromCapBackoffMs(fastCapBackoffMs);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
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

    public int getSlowCeiling() {
        return slowCeiling;
    }

    public int getFastCeiling() {
        return fastCeiling;
    }

    public YdbRetryPolicyConfig merge(@Nullable YdbTransactional transactionPolicy) {
        if (transactionPolicy == null) {
            return this;
        }
        return new YdbRetryPolicyConfig(
                enabled && transactionPolicy.enabled(),
                mergeOverride("maxAttempts", transactionPolicy.maxAttempts(), maxAttempts),
                mergeOverride(
                        "slowBackoffBaseMs", transactionPolicy.slowBackoffBaseMs(), slowBackoffBaseMs),
                mergeOverride(
                        "fastBackoffBaseMs", transactionPolicy.fastBackoffBaseMs(), fastBackoffBaseMs),
                mergeOverride(
                        "slowCapBackoffMs", transactionPolicy.slowCapBackoffMs(), slowCapBackoffMs),
                mergeOverride(
                        "fastCapBackoffMs", transactionPolicy.fastCapBackoffMs(), fastCapBackoffMs));
    }

    /**
     * Resolves a per-method override against the global value: {@code 0} inherits the global value,
     * a positive value overrides it, and a negative value is rejected.
     */
    private static int mergeOverride(String name, int candidate, int fallback) {
        if (candidate < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
        return candidate == 0 ? fallback : candidate;
    }

    /**
     * Ceiling on the exponent so that {@code baseMs * 2^ceiling} just reaches {@code capMs}.
     * Ported one-to-one from kotlin-exposed: {@code ceil(ln(capMs + 1) / ln(2))}.
     */
    static int ceilingFromCapBackoffMs(int capBackoffMs) {
        if (capBackoffMs <= 0) {
            return 0;
        }
        double value = capBackoffMs + 1.0d;
        return (int) Math.ceil(Math.log(value) / Math.log(2.0d));
    }
}
