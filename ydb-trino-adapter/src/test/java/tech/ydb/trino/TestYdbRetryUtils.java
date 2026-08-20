package tech.ydb.trino;

import org.junit.jupiter.api.Test;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.jdbc.exception.YdbStatusable;

import java.sql.SQLException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class TestYdbRetryUtils {
    @Test
    void testRetriesOnlyUnconditionalStatusesFromPinnedSdk() {
        assertThat(Arrays.stream(StatusCode.values())
                .filter(code -> YdbRetryUtils.isRetryable(new StatusSQLException(code))))
                .containsExactlyInAnyOrder(
                        StatusCode.ABORTED,
                        StatusCode.UNAVAILABLE,
                        StatusCode.OVERLOADED,
                        StatusCode.BAD_SESSION,
                        StatusCode.SESSION_BUSY,
                        StatusCode.CLIENT_RESOURCE_EXHAUSTED);
    }

    @Test
    void testFindsYdbStatusInCauseChain() {
        SQLException statusException = new StatusSQLException(StatusCode.ABORTED);

        assertThat(YdbRetryUtils.isRetryable(new SQLException("wrapper", statusException))).isTrue();
    }

    private static final class StatusSQLException extends SQLException implements YdbStatusable {
        private final Status status;

        private StatusSQLException(StatusCode statusCode) {
            this.status = Status.of(statusCode);
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }
}
