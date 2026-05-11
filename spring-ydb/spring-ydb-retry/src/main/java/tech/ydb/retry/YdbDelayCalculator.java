package tech.ydb.retry;

import org.springframework.lang.Nullable;
import tech.ydb.core.StatusCode;

public class YdbDelayCalculator {
    public static long calculateDelay(
            @Nullable StatusCode statusCode, YdbRetryPolicyConfig retryConfig, int attempt) {
        if (statusCode == null) {
            return 0;
        }

        return switch (statusCode) {
            case BAD_SESSION, SESSION_BUSY -> 0;
            case UNDETERMINED, ABORTED, CLIENT_CANCELLED, CLIENT_INTERNAL_ERROR -> delayWithFullJitter(
                    retryConfig.getFastBackoffBaseMs(),
                    retryConfig.getFastCapBackoffMs(),
                    retryConfig.getFastPow(),
                    attempt,
                    retryConfig);
            case UNAVAILABLE, TRANSPORT_UNAVAILABLE -> delayWithEqualJitter(
                    retryConfig.getFastBackoffBaseMs(),
                    retryConfig.getFastCapBackoffMs(),
                    retryConfig.getFastPow(),
                    attempt,
                    retryConfig);
            case OVERLOADED, CLIENT_RESOURCE_EXHAUSTED -> delayWithEqualJitter(
                    retryConfig.getSlowBackoffBaseMs(),
                    retryConfig.getSlowCapBackoffMs(),
                    retryConfig.getSlowPow(),
                    attempt,
                    retryConfig);
            default -> 0;
        };
    }

    static long calculateBackoff(int baseMs, int capMs, int pow, int attempt) {
        return Math.min((long) baseMs * (1L << Math.min(pow, attempt)), capMs);
    }

    private static long delayWithFullJitter(
            int baseMs, int capMs, int pow, int attempt, YdbRetryPolicyConfig retryConfig) {
        return retryConfig.getJitter(calculateBackoff(baseMs, capMs, pow, attempt));
    }

    private static long delayWithEqualJitter(
            int baseMs, int capMs, int pow, int attempt, YdbRetryPolicyConfig retryConfig) {
        long calculatedBackoff = calculateBackoff(baseMs, capMs, pow, attempt);
        long temp = calculatedBackoff / 2;
        return temp + calculatedBackoff % 2 + retryConfig.getJitter(temp);
    }
}
