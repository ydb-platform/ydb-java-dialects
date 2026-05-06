package tech.ydb.retry;

import org.springframework.lang.Nullable;
import tech.ydb.core.StatusCode;

public final class YdbRetryPolicy {
    public static boolean shouldRetry(@Nullable StatusCode statusCode, boolean isIdempotent) {
        return statusCode != null && statusCode.isRetryable(isIdempotent);
    }
}
