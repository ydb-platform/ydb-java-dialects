package tech.ydb.trino;

import tech.ydb.jdbc.exception.YdbStatusable;

import java.util.concurrent.ThreadLocalRandom;

public final class YdbRetryUtils {
    private static final long BASE_DELAY_MS = 20;
    private static final long MAX_DELAY_MS = 1000;

    private YdbRetryUtils() {
    }

    public static boolean isRetryable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof YdbStatusable statusable) {
                // false excludes statuses that are safe to retry only for idempotent operations.
                return statusable.getStatus().getCode().isRetryable(false);
            }
            current = current.getCause();
        }
        return false;
    }

    public static long calculateBackoff(int attempt) {
        long exponentialBackoff = BASE_DELAY_MS * (1L << Math.min(attempt, 10));
        long cappedBackoff = Math.min(exponentialBackoff, MAX_DELAY_MS);
        return ThreadLocalRandom.current().nextLong(0, cappedBackoff + 1);
    }
}
