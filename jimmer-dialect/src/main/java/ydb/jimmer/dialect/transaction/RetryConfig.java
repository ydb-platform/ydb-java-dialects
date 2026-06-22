package ydb.jimmer.dialect.transaction;

/**
 * The configuration class for transaction retries.
 * Most constants are taken from {@code spring-ydb-retry}'s {@code YdbRetryPolicyConfig.java}
 *
 * @param maxAttempts A maximum number of attempts for the transaction
 * @param slowBackoffBaseMs slow backoff to wait for slow transaction errors
 * @param fastBackoffBaseMs fast backoff to wait for fast transaction errors
 * @param slowCapBackoffMs maximum backoff for slow transaction errors
 * @param fastCapBackoffMs maximum backoff for fast transaction errors
 * @param backoffMultiplier A multiplier for a backoff for the next retry attempt
 * @param idempotent Are the operations in the transaction idempotent
 */
public record RetryConfig(
        int maxAttempts,
        int slowBackoffBaseMs,
        int fastBackoffBaseMs,
        int slowCapBackoffMs,
        int fastCapBackoffMs,
        double backoffMultiplier,
        boolean idempotent
) {
    public final static RetryConfig DEFAULT = new RetryConfig();

    public final static int DEFAULT_MAX_ATTEMPTS = 1;
    public static final int DEFAULT_SLOW_BACKOFF_BASE_MS = 50;
    public static final int DEFAULT_FAST_BACKOFF_BASE_MS = 5;
    public static final int DEFAULT_SLOW_CAP_BACKOFF_MS = 5_000;
    public static final int DEFAULT_FAST_CAP_BACKOFF_MS = 500;
    public final static double DEFAULT_BACKOFF_MULTIPLIER = 2;
    public final static boolean DEFAULT_IDEMPOTENT = false;

    public RetryConfig {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be a positive integer");
        }
        if (slowBackoffBaseMs < 0
                || fastBackoffBaseMs < 0
                || slowCapBackoffMs < 0
                || fastCapBackoffMs < 0) {
            throw new IllegalArgumentException("backoff values must be >= 0");
        }
        if (backoffMultiplier < 1) {
            throw new IllegalArgumentException("backoffMultiplier must be a positive integer");
        }
    }

    public RetryConfig() {
        this(DEFAULT_MAX_ATTEMPTS);
    }

    public RetryConfig(int maxAttempts) {
        this(
                maxAttempts,
                DEFAULT_SLOW_BACKOFF_BASE_MS,
                DEFAULT_FAST_BACKOFF_BASE_MS
        );
    }

    public RetryConfig(
            int maxAttempts,
            int slowBackoffBaseMs,
            int fastBackoffBaseMs
    ) {
        this(
                maxAttempts,
                slowBackoffBaseMs,
                fastBackoffBaseMs,
                DEFAULT_SLOW_CAP_BACKOFF_MS,
                DEFAULT_FAST_CAP_BACKOFF_MS,
                DEFAULT_BACKOFF_MULTIPLIER
        );
    }

    public RetryConfig(
            int maxAttempts,
            int slowBackoffBaseMs,
            int fastBackoffBaseMs,
            int slowCapBackoffMs,
            int fastCapBackoffMs
    ) {
        this(
                maxAttempts,
                slowBackoffBaseMs,
                fastBackoffBaseMs,
                slowCapBackoffMs,
                fastCapBackoffMs,
                DEFAULT_BACKOFF_MULTIPLIER
        );
    }

    public RetryConfig(
            int maxAttempts,
            int slowBackoffBaseMs,
            int fastBackoffBaseMs,
            int slowCapBackoffMs,
            int fastCapBackoffMs,
            boolean idempotent
    ) {
        this(
                maxAttempts,
                slowBackoffBaseMs,
                fastBackoffBaseMs,
                slowCapBackoffMs,
                fastCapBackoffMs,
                DEFAULT_BACKOFF_MULTIPLIER,
                idempotent
        );
    }

    public RetryConfig(
            int maxAttempts,
            int slowBackoffBaseMs,
            int fastBackoffBaseMs,
            int slowCapBackoffMs,
            int fastCapBackoffMs,
            double backoffMultiplier
    ) {
        this(
                maxAttempts,
                slowBackoffBaseMs,
                fastBackoffBaseMs,
                slowCapBackoffMs,
                fastCapBackoffMs,
                backoffMultiplier,
                DEFAULT_IDEMPOTENT
        );
    }
}
