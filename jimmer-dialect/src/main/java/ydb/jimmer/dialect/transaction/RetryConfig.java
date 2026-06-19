package ydb.jimmer.dialect.transaction;

public record RetryConfig(
        int maxAttempts,
        long retryDelayMs,
        int backoffMultiplier,
        boolean idempotent
) {
    public final static RetryConfig DEFAULT = new RetryConfig();

    private final static int DEFAULT_MAX_ATTEMPTS = 1;
    private final static int DEFAULT_RETRY_DELAY_MS = 0;
    private final static int DEFAULT_BACKOFF_MULTIPLIER = 2;
    private final static boolean DEFAULT_IDEMPOTENT = false;

    public RetryConfig {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be a positive integer");
        } else if (retryDelayMs < 0) {
            throw new IllegalArgumentException("retryDelayMs must not be negative");
        }
    }

    public RetryConfig() {
        this(DEFAULT_MAX_ATTEMPTS, DEFAULT_RETRY_DELAY_MS);
    }

    public RetryConfig(int maxAttempts, long retryDelayMs) {
        this(maxAttempts, retryDelayMs, DEFAULT_BACKOFF_MULTIPLIER);
    }

    public RetryConfig(int maxAttempts, long retryDelayMs, boolean idempotent) {
        this(maxAttempts, retryDelayMs, DEFAULT_BACKOFF_MULTIPLIER, idempotent);
    }

    public RetryConfig(int maxAttempts, long retryDelayMs, int backoffMultiplier) {
        this(maxAttempts, retryDelayMs, backoffMultiplier, DEFAULT_IDEMPOTENT);
    }
}
