package tech.ydb.retry;

import java.util.List;
import org.junit.jupiter.api.Test;
import tech.ydb.core.StatusCode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.ydb.core.StatusCode.ABORTED;
import static tech.ydb.core.StatusCode.BAD_REQUEST;
import static tech.ydb.core.StatusCode.BAD_SESSION;
import static tech.ydb.core.StatusCode.CANCELLED;
import static tech.ydb.core.StatusCode.CLIENT_CANCELLED;
import static tech.ydb.core.StatusCode.CLIENT_INTERNAL_ERROR;
import static tech.ydb.core.StatusCode.CLIENT_RESOURCE_EXHAUSTED;
import static tech.ydb.core.StatusCode.EXTERNAL_ERROR;
import static tech.ydb.core.StatusCode.GENERIC_ERROR;
import static tech.ydb.core.StatusCode.INTERNAL_ERROR;
import static tech.ydb.core.StatusCode.NOT_FOUND;
import static tech.ydb.core.StatusCode.OVERLOADED;
import static tech.ydb.core.StatusCode.SCHEME_ERROR;
import static tech.ydb.core.StatusCode.SESSION_BUSY;
import static tech.ydb.core.StatusCode.SESSION_EXPIRED;
import static tech.ydb.core.StatusCode.TIMEOUT;
import static tech.ydb.core.StatusCode.TRANSPORT_UNAVAILABLE;
import static tech.ydb.core.StatusCode.UNAUTHORIZED;
import static tech.ydb.core.StatusCode.UNAVAILABLE;
import static tech.ydb.core.StatusCode.UNDETERMINED;
import static tech.ydb.core.StatusCode.UNSUPPORTED;

class YdbRetryPolicyTest {
    @Test
    void shouldRetryAlwaysRetryableStatusesRegardlessOfIdempotence() {
        List<StatusCode> alwaysRetryable =
                List.of(
                        BAD_SESSION, SESSION_BUSY, ABORTED, UNAVAILABLE, OVERLOADED, CLIENT_RESOURCE_EXHAUSTED);

        for (StatusCode code : alwaysRetryable) {
            assertTrue(
                    YdbRetryPolicy.shouldRetry(code, false), "Should retry " + code + " when not idempotent");
            assertTrue(
                    YdbRetryPolicy.shouldRetry(code, true), "Should retry " + code + " when idempotent");
        }
    }

    @Test
    void shouldNotRetryIdempotentOnlyStatusesWhenNotIdempotent() {
        List<StatusCode> idempotentOnly =
                List.of(CLIENT_CANCELLED, CLIENT_INTERNAL_ERROR, TRANSPORT_UNAVAILABLE, UNDETERMINED);

        for (StatusCode code : idempotentOnly) {
            assertFalse(
                    YdbRetryPolicy.shouldRetry(code, false),
                    "Should not retry " + code + " when not idempotent");
        }
    }

    @Test
    void shouldRetryIdempotentOnlyStatusesWhenIdempotent() {
        List<StatusCode> idempotentOnly =
                List.of(CLIENT_CANCELLED, CLIENT_INTERNAL_ERROR, TRANSPORT_UNAVAILABLE, UNDETERMINED);

        for (StatusCode code : idempotentOnly) {
            assertTrue(
                    YdbRetryPolicy.shouldRetry(code, true), "Should retry " + code + " when idempotent");
        }
    }

    @Test
    void shouldNotRetryNonRetryableStatuses() {
        List<StatusCode> nonRetryable =
                List.of(
                        StatusCode.SUCCESS,
                        BAD_REQUEST,
                        UNAUTHORIZED,
                        INTERNAL_ERROR,
                        SCHEME_ERROR,
                        GENERIC_ERROR,
                        NOT_FOUND,
                        UNSUPPORTED,
                        CANCELLED,
                        EXTERNAL_ERROR,
                        TIMEOUT,
                        SESSION_EXPIRED);

        for (StatusCode code : nonRetryable) {
            assertFalse(
                    YdbRetryPolicy.shouldRetry(code, false),
                    "Should not retry " + code + " when not idempotent");
            assertFalse(
                    YdbRetryPolicy.shouldRetry(code, true), "Should not retry " + code + " when idempotent");
        }
    }

    @Test
    void shouldNotRetryNullStatusCode() {
        assertFalse(YdbRetryPolicy.shouldRetry(null, false));
        assertFalse(YdbRetryPolicy.shouldRetry(null, true));
    }
}
