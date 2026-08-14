package tech.ydb.trino;

import io.trino.spi.TrinoException;
import tech.ydb.jdbc.exception.YdbStatusable;

import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;

public final class YdbRetryUtils {
    private static final int MAX_RETRIES = 10;
    private static final long BASE_DELAY_MS = 20;
    private static final long MAX_DELAY_MS = 1000;

    private YdbRetryUtils() {
    }

    @FunctionalInterface
    public interface SqlRunnable {
        void run() throws SQLException;
    }

    public static void withRetry(SqlRunnable action) throws SQLException {
        SQLException lastException = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                action.run();
                return;
            }
            catch (SQLException e) {
                lastException = e;
                if (!isRetryable(e)) {
                    throw e;
                }

                long delay = calculateBackoff(attempt);
                try {
                    Thread.sleep(delay);
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new TrinoException(JDBC_ERROR, ie);
                }
            }
        }
        throw new TrinoException(JDBC_ERROR, lastException);
    }

    public static boolean isRetryable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof YdbStatusable statusable) {
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
