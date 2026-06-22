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
 * @param slowCeiling Ceiling on the exponent so that {@code slowBackoffBaseMs * multiplier^ceiling}
 *                   just reaches {@code slowCapBackoffMs}
 * @param fastCeiling Ceiling on the exponent so that {@code fastBackoffBaseMs * multiplier^ceiling}
 *                   just reaches {@code fastCapBackoffMs}
 * @param backoffMultiplier A multiplier for a backoff for the next retry attempt
 * @param idempotent Are the operations in the transaction idempotent
 */
public record RetryConfig(
        int maxAttempts,
        int slowBackoffBaseMs,
        int fastBackoffBaseMs,
        int slowCapBackoffMs,
        int fastCapBackoffMs,
        int slowCeiling,
        int fastCeiling,
        double backoffMultiplier,
        boolean idempotent
) {
    public final static RetryConfig DEFAULT = new Builder().build();

    public RetryConfig {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        if (slowBackoffBaseMs < 0 || fastBackoffBaseMs < 0) {
            throw new IllegalArgumentException("backoff values must be >= 0");
        }
        if (slowCeiling < 1 || fastCeiling < 1) {
            throw new IllegalArgumentException("ceiling values must be >= 1");
        }
        if (backoffMultiplier < 1) {
            throw new IllegalArgumentException("backoffMultiplier must be >= 1");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int maxAttempts = 1;
        private int slowBackoffBaseMs = 50;
        private int fastBackoffBaseMs = 5;
        private int slowCapBackoffMs = 5_000;
        private int fastCapBackoffMs = 500;
        private double backoffMultiplier = 2.0;
        private boolean idempotent = false;

        public Builder maxAttempts(int value) {
            this.maxAttempts = value;
            return this;
        }

        public Builder backoffBaseMs(int value) {
            return this.slowBackoffBaseMs(value).fastBackoffBaseMs(value);
        }

        public Builder slowBackoffBaseMs(int value) {
            this.slowBackoffBaseMs = value;
            return this;
        }

        public Builder fastBackoffBaseMs(int value) {
            this.fastBackoffBaseMs = value;
            return this;
        }

        public Builder capBackoffMs(int value) {
            return this.slowCapBackoffMs(value).fastCapBackoffMs(value);
        }

        public Builder slowCapBackoffMs(int value) {
            this.slowCapBackoffMs = value;
            return this;
        }

        public Builder fastCapBackoffMs(int value) {
            this.fastCapBackoffMs = value;
            return this;
        }

        public Builder backoffMultiplier(double value) {
            this.backoffMultiplier = value;
            return this;
        }

        public Builder idempotent(boolean value) {
            this.idempotent = value;
            return this;
        }

        public RetryConfig build() {
            if (slowCapBackoffMs < 0 || fastCapBackoffMs < 0) {
                throw new IllegalArgumentException("backoff cap values must be >= 0");
            }
            if (backoffMultiplier < 1) {
                throw new IllegalArgumentException("backoffMultiplier must be >= 1");
            }

            int slowCeiling = ceilingFromCapBackoffMs(slowCapBackoffMs, backoffMultiplier);
            int fastCeiling = ceilingFromCapBackoffMs(fastCapBackoffMs, backoffMultiplier);

            return new RetryConfig(
                    maxAttempts, slowBackoffBaseMs, fastBackoffBaseMs,
                    slowCapBackoffMs, fastCapBackoffMs, slowCeiling, fastCeiling,
                    backoffMultiplier, idempotent);
        }

        /**
         * Ceiling on the exponent so that {@code baseMs * multiplier^ceiling} just reaches {@code capMs}.
         * {@code ceil(ln(capMs + multiplier - 1) / ln(multiplier))}.
         */
        private static int ceilingFromCapBackoffMs(int capBackoffMs, double multiplier) {
            if (capBackoffMs <= 0) {
                return 0;
            }
            double value = capBackoffMs + (multiplier - 1);
            return (int) Math.ceil(Math.log(value) / Math.log(multiplier));
        }
    }
}
