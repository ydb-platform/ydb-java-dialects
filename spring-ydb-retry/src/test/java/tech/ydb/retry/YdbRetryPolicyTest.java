package tech.ydb.retry;

import java.sql.SQLException;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.ydb.retry.YdbVendorCode.ABORTED;
import static tech.ydb.retry.YdbVendorCode.BAD_SESSION;
import static tech.ydb.retry.YdbVendorCode.CLIENT_GRPC_ERROR;
import static tech.ydb.retry.YdbVendorCode.CLIENT_RESOURCE_EXHAUSTED;
import static tech.ydb.retry.YdbVendorCode.NOT_FOUND;
import static tech.ydb.retry.YdbVendorCode.OVERLOADED;
import static tech.ydb.retry.YdbVendorCode.PRECONDITION_FAILED;
import static tech.ydb.retry.YdbVendorCode.SESSION_BUSY;
import static tech.ydb.retry.YdbVendorCode.SESSION_EXPIRED;
import static tech.ydb.retry.YdbVendorCode.TIMEOUT;
import static tech.ydb.retry.YdbVendorCode.TRANSPORT_UNAVAILABLE;
import static tech.ydb.retry.YdbVendorCode.UNAVAILABLE;
import static tech.ydb.retry.YdbVendorCode.UNDETERMINED;

class YdbRetryPolicyTest {

    private static final YdbRetryPolicyConfig CONFIG = new YdbRetryPolicyConfig();

    @Test
    void shouldClassifyTransientCodes() {
        List<Integer> transient0 =
                List.of(ABORTED, UNAVAILABLE, OVERLOADED, CLIENT_RESOURCE_EXHAUSTED, BAD_SESSION,
                        SESSION_BUSY);

        for (int code : transient0) {
            assertTrue(YdbRetryPolicy.isTransientVendorCode(code),
                    "Vendor code " + code + " must be transient");
        }
    }

    @Test
    void shouldNotClassifyIdempotentOnlyCodesAsTransient() {
        for (int code : List.of(UNDETERMINED, TRANSPORT_UNAVAILABLE, CLIENT_GRPC_ERROR,
                SESSION_EXPIRED)) {
            assertFalse(YdbRetryPolicy.isTransientVendorCode(code),
                    "Vendor code " + code + " must not be transient");
        }
    }

    @Test
    void shouldRetryTransientCodesRegardlessOfIdempotence() {
        for (int code : List.of(BAD_SESSION, SESSION_BUSY, ABORTED, UNAVAILABLE, OVERLOADED,
                CLIENT_RESOURCE_EXHAUSTED)) {
            assertTrue(YdbRetryPolicy.getNextRetryDelayMs(code, 0, CONFIG, false).isPresent(),
                    "Should retry transient " + code + " even when not idempotent");
            assertTrue(YdbRetryPolicy.getNextRetryDelayMs(code, 0, CONFIG, true).isPresent(),
                    "Should retry transient " + code + " when idempotent");
        }
    }

    @Test
    void shouldRetryIdempotentOnlyCodesOnlyWhenIdempotent() {
        for (int code : List.of(UNDETERMINED, TRANSPORT_UNAVAILABLE, CLIENT_GRPC_ERROR,
                SESSION_EXPIRED)) {
            assertTrue(YdbRetryPolicy.getNextRetryDelayMs(code, 0, CONFIG, false).isEmpty(),
                    "Must not retry idempotent-only " + code + " when not idempotent");
            assertTrue(YdbRetryPolicy.getNextRetryDelayMs(code, 0, CONFIG, true).isPresent(),
                    "Must retry idempotent-only " + code + " when idempotent");
        }
    }

    @Test
    void shouldNeverRetryHardErrors() {
        for (int code : List.of(TIMEOUT, PRECONDITION_FAILED, NOT_FOUND, 0, 999_999)) {
            assertTrue(YdbRetryPolicy.getNextRetryDelayMs(code, 0, CONFIG, false).isEmpty(),
                    "Must not retry hard error " + code + " when not idempotent");
            assertTrue(YdbRetryPolicy.getNextRetryDelayMs(code, 0, CONFIG, true).isEmpty(),
                    "Must not retry hard error " + code + " when idempotent");
        }
    }

    @Test
    void shouldUseZeroDelayForSessionStatuses() {
        assertEquals(0L,
                YdbRetryPolicy.getNextRetryDelayMs(BAD_SESSION, 0, CONFIG, false).getAsLong());
        assertEquals(0L,
                YdbRetryPolicy.getNextRetryDelayMs(SESSION_BUSY, 0, CONFIG, false).getAsLong());
        assertEquals(0L,
                YdbRetryPolicy.getNextRetryDelayMs(SESSION_EXPIRED, 0, CONFIG, true).getAsLong());
    }

    @Test
    void shouldReturnEmptyWhenAttemptBudgetExhausted() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 2, 0, 0, 0, 0);
        // maxAttempts counts the initial execution, so with budget=2 only the first
        // failure (attempt=0) is allowed to schedule a retry; the second one exhausts the budget.
        assertTrue(YdbRetryPolicy.getNextRetryDelayMs(ABORTED, 0, config, false).isPresent());
        assertTrue(YdbRetryPolicy.getNextRetryDelayMs(ABORTED, 1, config, false).isEmpty());
        assertTrue(YdbRetryPolicy.getNextRetryDelayMs(ABORTED, 2, config, false).isEmpty());
    }

    @Test
    void shouldExtractZeroForNonYdbThrowable() {
        assertEquals(0, YdbRetryPolicy.extractVendorCode(new IllegalStateException("not ydb")));
    }

    @Test
    void shouldExtractZeroForNullErrorCodeSqlException() {
        assertEquals(0, YdbRetryPolicy.extractVendorCode(new SQLException("no code", null, 0)));
    }

    @Test
    void shouldExtractVendorCodeFromDirectSqlException() {
        assertEquals(BAD_SESSION,
                YdbRetryPolicy.extractVendorCode(new SQLException("ydb", null, BAD_SESSION)));
    }

    @Test
    void shouldExtractVendorCodeFromCauseChain() {
        Throwable wrapped = new RuntimeException("outer",
                new RuntimeException("middle", new SQLException("ydb", null, ABORTED)));
        assertEquals(ABORTED, YdbRetryPolicy.extractVendorCode(wrapped));
    }

    @Test
    void shouldReturnEmptyDelayForZeroVendorCode() {
        OptionalLong delay = YdbRetryPolicy.getNextRetryDelayMs(0, 0, CONFIG, true);
        assertTrue(delay.isEmpty());
    }
}
