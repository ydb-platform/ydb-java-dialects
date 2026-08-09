package tech.ydb.trino;

import io.trino.spi.TrinoException;

import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;

public final class YdbRetryUtils {
    private static final int MAX_RETRIES = 10;
    private static final long BASE_DELAY_MS = 20;
    private static final long MAX_DELAY_MS = 1000;

    /**
     * These are taken from <a href="https://ydb.tech/docs/en/reference/ydb-sdk/ydb-status-codes?version=v26.1">here</a>.
     */
    private static final Set<Integer> RETRYABLE_VENDOR_CODES = Set.of(
            YdbVendorCode.ABORTED,
            YdbVendorCode.UNAVAILABLE,
            YdbVendorCode.OVERLOADED,
            YdbVendorCode.CLIENT_RESOURCE_EXHAUSTED,
            YdbVendorCode.BAD_SESSION,
            YdbVendorCode.SESSION_BUSY,
            YdbVendorCode.SESSION_EXPIRED,
            YdbVendorCode.TIMEOUT,
            YdbVendorCode.UNDETERMINED,
            YdbVendorCode.TRANSPORT_UNAVAILABLE,
            YdbVendorCode.CLIENT_GRPC_ERROR);

    private YdbRetryUtils() {
    }

    @FunctionalInterface
    public interface SqlRunnable {
        void run() throws SQLException;
    }

    @FunctionalInterface
    public interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    public static void withRetry(SqlRunnable action) throws SQLException {
        executeWithRetry(() -> {
            action.run();
            return null;
        });
    }

    public static <T> T withRetryReturn(SqlSupplier<T> action) throws SQLException {
        return executeWithRetry(action);
    }

    private static <T> T executeWithRetry(SqlSupplier<T> action) throws SQLException {
        SQLException lastException = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return action.get();
            } catch (SQLException e) {
                lastException = e;
                int vendorCode = extractVendorCode(e);

                if (!isRetryable(vendorCode)) {
                    throw e;
                }

                long delay = calculateBackoff(attempt);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new TrinoException(JDBC_ERROR, ie);
                }
            }
        }
        throw new TrinoException(JDBC_ERROR, lastException);
    }

    public static int extractVendorCode(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                int vendorCode = sqlException.getErrorCode();
                if (vendorCode != 0) {
                    return vendorCode;
                }
            }
            current = current.getCause();
        }
        return 0;
    }

    public static boolean isRetryable(int vendorCode) {
        return RETRYABLE_VENDOR_CODES.contains(vendorCode);
    }

    public static long calculateBackoff(int attempt) {
        long exponentialBackoff = BASE_DELAY_MS * (1L << Math.min(attempt, 10));
        long cappedBackoff = Math.min(exponentialBackoff, MAX_DELAY_MS);
        return ThreadLocalRandom.current().nextLong(0, cappedBackoff + 1);
    }
}