package tech.ydb.trino;

import io.trino.spi.TrinoException;

import java.sql.SQLException;
import java.time.Duration;

import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;

public final class YdbRetryUtils {
    private static final int MAX_RETRIES = 4;
    private static final int BASE_DELAY_MILLIS = 20;
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
            } catch (SQLException e) {
                lastException = e;
                String message = e.getMessage();

                if (message != null) {
                    Duration delay = Duration.ofMillis(BASE_DELAY_MILLIS).multipliedBy(1L << attempt);

                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new TrinoException(JDBC_ERROR, "InterruptedException", ie);
                    }
                } else {
                    throw e;
                }
            }
        }

        throw new TrinoException(JDBC_ERROR, lastException);
    }
}
