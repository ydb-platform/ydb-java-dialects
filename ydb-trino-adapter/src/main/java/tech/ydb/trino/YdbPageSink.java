package tech.ydb.trino;

import io.airlift.slice.Slice;
import io.trino.plugin.jdbc.JdbcClient;
import io.trino.plugin.jdbc.JdbcOutputTableHandle;
import io.trino.plugin.jdbc.JdbcPageSink;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.Page;
import io.trino.spi.TrinoException;
import io.trino.spi.connector.ConnectorPageSink;
import io.trino.spi.connector.ConnectorPageSinkId;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTransactionHandle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class YdbPageSink
        implements ConnectorPageSink
{
    private final ConnectorSession session;
    private final JdbcOutputTableHandle outputTableHandle;
    private final JdbcClient jdbcClient;
    private final ConnectorPageSinkId pageSinkId;
    private final RemoteQueryModifier remoteQueryModifier;

    private final List<Page> bufferedPages = new ArrayList<>();
    private boolean finished = false;

    public YdbPageSink(
            @SuppressWarnings("unused") ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            JdbcOutputTableHandle outputTableHandle,
            JdbcClient jdbcClient,
            ConnectorPageSinkId pageSinkId,
            RemoteQueryModifier remoteQueryModifier
    ) {
        this.session = session;
        this.outputTableHandle = outputTableHandle;
        this.jdbcClient = jdbcClient;
        this.pageSinkId = pageSinkId;
        this.remoteQueryModifier = remoteQueryModifier;
    }

    @Override
    public CompletableFuture<?> appendPage(Page page) {
        if (finished) {
            throw new IllegalStateException();
        }
        bufferedPages.add(page);
        return NOT_BLOCKED;
    }

    @Override
    public CompletableFuture<Collection<Slice>> finish() {
        finished = true;

        int maxAttempts = 10;
        Exception lastException = null;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            JdbcPageSink delegate = null;
            try {
                delegate = new JdbcPageSink(
                        session,
                        outputTableHandle,
                        jdbcClient,
                        pageSinkId,
                        remoteQueryModifier,
                        JdbcClient::buildInsertSql);

                for (Page page : bufferedPages) {
                    delegate.appendPage(page);
                }

                Collection<Slice> result = delegate.finish().join();
                return completedFuture(result);
            }
            catch (Exception e) {
                if (delegate != null) {
                    try {
                        delegate.abort();
                    }
                    catch (Exception abortError) {
                        e.addSuppressed(abortError);
                    }
                }

                if (!isRetryableError(e)) {
                    throw e;
                }

                lastException = e;

                long delay = YdbRetryUtils.calculateBackoff(attempt);

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

    private boolean isRetryableError(Throwable error) {
        int vendorCode = YdbRetryUtils.extractVendorCode(error);
        return YdbRetryUtils.isRetryable(vendorCode);
    }

    @Override
    public void abort() {
        finished = true;
        bufferedPages.clear();
    }
}