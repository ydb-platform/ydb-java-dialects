package tech.ydb.trino;

import com.google.inject.Inject;
import io.trino.plugin.jdbc.JdbcClient;
import io.trino.plugin.jdbc.JdbcOutputTableHandle;
import io.trino.plugin.jdbc.JdbcPageSink;
import io.trino.plugin.jdbc.QueryBuilder;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.connector.ConnectorInsertTableHandle;
import io.trino.spi.connector.ConnectorMergeSink;
import io.trino.spi.connector.ConnectorMergeTableHandle;
import io.trino.spi.connector.ConnectorOutputTableHandle;
import io.trino.spi.connector.ConnectorPageSink;
import io.trino.spi.connector.ConnectorPageSinkId;
import io.trino.spi.connector.ConnectorPageSinkProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTableCredentials;
import io.trino.spi.connector.ConnectorTransactionHandle;

import java.util.Optional;

public record YdbPageSinkProvider(
        JdbcClient jdbcClient,
        RemoteQueryModifier remoteQueryModifier,
        QueryBuilder queryBuilder
) implements ConnectorPageSinkProvider {
    @Inject
    public YdbPageSinkProvider {

    }

    @Override
    public ConnectorPageSink createPageSink(
            ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorOutputTableHandle tableHandle,
            Optional<ConnectorTableCredentials> tableCredentials,
            ConnectorPageSinkId pageSinkId) {
        return new JdbcPageSink(
                session,
                (JdbcOutputTableHandle) tableHandle,
                jdbcClient,
                pageSinkId,
                remoteQueryModifier,
                JdbcClient::buildInsertSql);
    }

    @Override
    public ConnectorPageSink createPageSink(
            ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorInsertTableHandle tableHandle,
            Optional<ConnectorTableCredentials> tableCredentials,
            ConnectorPageSinkId pageSinkId) {
        return new JdbcPageSink(
                session,
                (JdbcOutputTableHandle) tableHandle,
                jdbcClient,
                pageSinkId,
                remoteQueryModifier,
                JdbcClient::buildInsertSql);
    }

    @Override
    public ConnectorMergeSink createMergeSink(
            ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorMergeTableHandle mergeHandle,
            Optional<ConnectorTableCredentials> tableCredentials,
            ConnectorPageSinkId pageSinkId) {
        return new YdbMergeSink(
                transactionHandle,
                session,
                mergeHandle,
                jdbcClient,
                pageSinkId,
                remoteQueryModifier,
                queryBuilder);
    }
}
