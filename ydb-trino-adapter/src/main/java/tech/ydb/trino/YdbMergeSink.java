package tech.ydb.trino;

import com.google.common.collect.ImmutableList;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.plugin.jdbc.BooleanWriteFunction;
import io.trino.plugin.jdbc.DoubleWriteFunction;
import io.trino.plugin.jdbc.JdbcClient;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcMergeTableHandle;
import io.trino.plugin.jdbc.JdbcOutputTableHandle;
import io.trino.plugin.jdbc.LongWriteFunction;
import io.trino.plugin.jdbc.ObjectWriteFunction;
import io.trino.plugin.jdbc.QueryBuilder;
import io.trino.plugin.jdbc.SliceWriteFunction;
import io.trino.plugin.jdbc.WriteFunction;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.Page;
import io.trino.spi.TrinoException;
import io.trino.spi.block.Block;
import io.trino.spi.block.RowBlock;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorMergeSink;
import io.trino.spi.connector.ConnectorMergeTableHandle;
import io.trino.spi.connector.ConnectorPageSinkId;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.type.Type;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;
import static io.trino.plugin.jdbc.JdbcWriteSessionProperties.getWriteBatchSize;
import static io.trino.spi.StandardErrorCode.EXCEEDED_LOCAL_MEMORY_LIMIT;
import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.spi.type.TinyintType.TINYINT;
import static java.util.concurrent.CompletableFuture.completedFuture;

/*
  Code partly borrowed from JdbcMergeSink. Key differences are:
  - We do not write into the target table on each storeMergedRows call; instead, we store the necessary pages
    and apply them in one transaction when finish() is called. Without it, I had serious atomicity problems and flaky tests.
  - We retry the complete transaction after a retryable pre-commit YDB failure.
  - We do not use temporary tables unlike base Trino implementation, but write straight to YDB target.
 */
public class YdbMergeSink implements ConnectorMergeSink {
    private final ConnectorSession session;
    private final JdbcMergeTableHandle mergeHandle;
    private final JdbcClient jdbcClient;
    private final ConnectorPageSinkId pageSinkId;
    private final RemoteQueryModifier remoteQueryModifier;
    private final int maxBatchSize;
    private final long maxBufferedBytes;

    private final List<Page> bufferedPages = new ArrayList<>();
    private long bufferedBytes;
    private boolean finished = false;

    public YdbMergeSink(
            @SuppressWarnings("unused") ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorMergeTableHandle mergeTableHandle,
            JdbcClient jdbcClient,
            ConnectorPageSinkId pageSinkId,
            RemoteQueryModifier remoteQueryModifier,
            @SuppressWarnings("unused") QueryBuilder queryBuilder,
            long maxBufferedBytes
    ) {
        checkArgument(maxBufferedBytes > 0, "maxBufferedBytes must be greater than zero");
        this.session = session;
        this.mergeHandle = (JdbcMergeTableHandle) mergeTableHandle;
        this.jdbcClient = jdbcClient;
        this.pageSinkId = pageSinkId;
        this.remoteQueryModifier = remoteQueryModifier;
        this.maxBatchSize = getWriteBatchSize(session);
        this.maxBufferedBytes = maxBufferedBytes;
    }

    @Override
    public void storeMergedRows(Page page) {
        if (finished) {
            throw new IllegalStateException();
        }
        long retainedBytes = page.getRetainedSizeInBytes();
        if (retainedBytes > maxBufferedBytes - bufferedBytes) {
            finished = true;
            clearBufferedPages();
            throw new TrinoException(
                    EXCEEDED_LOCAL_MEMORY_LIMIT,
                    "YDB MERGE buffered input exceeds merge.max-buffer-size of " + maxBufferedBytes + " bytes");
        }
        bufferedPages.add(page);
        bufferedBytes += retainedBytes;
    }

    @Override
    public CompletableFuture<Collection<Slice>> finish() {
        if (finished) {
            throw new IllegalStateException();
        }
        finished = true;
        try {
            return finishWithRetry();
        }
        finally {
            clearBufferedPages();
        }
    }

    private CompletableFuture<Collection<Slice>> finishWithRetry() {
        int maxAttempts = 10;
        Exception lastException = null;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Connection connection = null;
            boolean commitAttempted = false;
            boolean committed = false;
            try {
                connection = openConnection();
                executeMergeInTransaction(connection);
                commitAttempted = true;
                connection.commit();
                committed = true;
                Connection committedConnection = connection;
                connection = null;
                committedConnection.close();

                Slice value = Slices.allocate(Long.BYTES);
                value.setLong(0, pageSinkId.getId());
                return completedFuture(ImmutableList.of(value));
            }
            catch (Exception e) {
                if (connection != null) {
                    if (!committed) {
                        try {
                            connection.rollback();
                        }
                        catch (SQLException rollbackError) {
                            e.addSuppressed(rollbackError);
                        }
                    }
                    try {
                        connection.close();
                    }
                    catch (SQLException closeError) {
                        e.addSuppressed(closeError);
                    }
                }

                if (committed) {
                    throw new TrinoException(JDBC_ERROR, "YDB MERGE committed, but closing its connection failed", e);
                }

                if (commitAttempted) {
                    throw new TrinoException(
                            JDBC_ERROR,
                            "YDB MERGE commit outcome is unknown; operation was not retried to avoid duplicate writes",
                            e);
                }

                if (!YdbRetryUtils.isRetryable(e) && !isRejectedDriverSessionAcquisition(e)) {
                    throw new TrinoException(JDBC_ERROR, e);
                }

                lastException = e;

                long delay = YdbRetryUtils.calculateBackoff(attempt);

                try {
                    Thread.sleep(delay);
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    e.addSuppressed(ie);
                    throw new TrinoException(JDBC_ERROR, "Interrupted while waiting to retry YDB MERGE", e);
                }
            }
        }

        throw new TrinoException(JDBC_ERROR, lastException);
    }

    private Connection openConnection() throws SQLException {
        JdbcOutputTableHandle outputHandle = mergeHandle.getOutputTableHandle();
        Connection connection = jdbcClient.getConnection(session, outputHandle);
        try {
            connection.setAutoCommit(false);
            return connection;
        }
        catch (SQLException e) {
            try {
                connection.close();
            }
            catch (SQLException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }
    }

    private void executeMergeInTransaction(Connection connection) throws SQLException {
        JdbcOutputTableHandle outputHandle = mergeHandle.getOutputTableHandle();
        List<JdbcColumnHandle> primaryKeys = mergeHandle.getPrimaryKeys();
        int columnCount = outputHandle.getColumnNames().size();
        List<JdbcColumnHandle> columns = mergeHandle.getDataColumns();

        validateMergeOperations(columnCount);
        executeDeleteOperations(connection, primaryKeys, columnCount);
        executeUpdateOperations(connection, primaryKeys, columns, columnCount);
        executeInsertOperations(connection, outputHandle, columnCount);
    }

    private void executeDeleteOperations(
            Connection connection,
            List<JdbcColumnHandle> primaryKeys,
            int columnCount
    ) throws SQLException {
        if (!containsOperation(columnCount, DELETE_OPERATION_NUMBER)) {
            return;
        }

        String tableName = mergeHandle.getTableHandle().getRequiredNamedRelation().getRemoteTableName().getTableName();
        StringBuilder deleteSql = new StringBuilder("DELETE FROM ");
        deleteSql.append(jdbcClient.quoted(tableName));
        deleteSql.append(" WHERE ");

        for (int i = 0; i < primaryKeys.size(); i++) {
            if (i > 0) {
                deleteSql.append(" AND ");
            }
            JdbcColumnHandle pk = primaryKeys.get(i);
            deleteSql.append(jdbcClient.quoted(pk.getColumnName()));
            deleteSql.append(" = ?");
        }

        String sql = remoteQueryModifier.apply(session, deleteSql.toString());
        List<Type> primaryKeyTypes = primaryKeys.stream()
                .map(JdbcColumnHandle::getColumnType)
                .toList();
        List<WriteFunction> primaryKeyWriters = getColumnWriters(primaryKeyTypes);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int batchSize = 0;
            for (Page page : bufferedPages) {
                Block operationBlock = page.getBlock(columnCount);
                List<Block> rowIdFields = null;
                for (int pos = 0; pos < page.getPositionCount(); pos++) {
                    if (TINYINT.getByte(operationBlock, pos) != DELETE_OPERATION_NUMBER) {
                        continue;
                    }
                    if (rowIdFields == null) {
                        rowIdFields = RowBlock.getRowFieldsFromBlock(page.getBlock(columnCount + 2));
                    }
                    for (int i = 0; i < primaryKeys.size(); i++) {
                        setParameter(
                                stmt,
                                i + 1,
                                rowIdFields.get(i),
                                pos,
                                primaryKeyTypes.get(i),
                                primaryKeyWriters.get(i));
                    }
                    batchSize = addToBatch(stmt, batchSize);
                }
            }
            executeRemainingBatch(stmt, batchSize);
        }
    }

    private void executeUpdateOperations(
            Connection connection,
            List<JdbcColumnHandle> primaryKeys,
            List<JdbcColumnHandle> columns,
            int columnCount
    ) throws SQLException {
        if (!containsOperation(columnCount, UPDATE_OPERATION_NUMBER)) {
            return;
        }

        String tableName = mergeHandle.getTableHandle().getRequiredNamedRelation().getRemoteTableName().getTableName();
        List<Type> primaryKeyTypes = primaryKeys.stream()
                .map(JdbcColumnHandle::getColumnType)
                .toList();
        List<WriteFunction> primaryKeyWriters = getColumnWriters(primaryKeyTypes);

        for (Map.Entry<Integer, Collection<ColumnHandle>> entry : mergeHandle.getUpdateCaseColumns().entrySet()) {
            int caseNumber = entry.getKey();
            Collection<ColumnHandle> updateColumns = entry.getValue();
            if (updateColumns == null || updateColumns.isEmpty() || !containsUpdateCase(columnCount, caseNumber)) {
                continue;
            }

            StringBuilder updateSql = new StringBuilder("UPDATE ");
            updateSql.append(jdbcClient.quoted(tableName));
            updateSql.append(" SET ");

            Set<Integer> updateChannelsSet = updateColumns.stream()
                    .map(JdbcColumnHandle.class::cast)
                    .map(columns::indexOf)
                    .collect(toImmutableSet());

            List<Integer> updateChannels = new ArrayList<>();
            List<JdbcColumnHandle> updateColumnList = new ArrayList<>();
            for (int channel = 0; channel < columns.size(); channel++) {
                if (updateChannelsSet.contains(channel)) {
                    updateChannels.add(channel);
                    updateColumnList.add(columns.get(channel));
                }
            }

            for (int i = 0; i < updateColumnList.size(); i++) {
                if (i > 0) {
                    updateSql.append(", ");
                }
                JdbcColumnHandle col = updateColumnList.get(i);
                updateSql.append(jdbcClient.quoted(col.getColumnName()));
                updateSql.append(" = ?");
            }

            updateSql.append(" WHERE ");
            for (int i = 0; i < primaryKeys.size(); i++) {
                if (i > 0) {
                    updateSql.append(" AND ");
                }
                JdbcColumnHandle pk = primaryKeys.get(i);
                updateSql.append(jdbcClient.quoted(pk.getColumnName()));
                updateSql.append(" = ?");
            }

            String sql = remoteQueryModifier.apply(session, updateSql.toString());
            List<Type> updateColumnTypes = updateColumnList.stream()
                    .map(JdbcColumnHandle::getColumnType)
                    .toList();
            List<WriteFunction> updateColumnWriters = getColumnWriters(updateColumnTypes);

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                int batchSize = 0;
                for (Page page : bufferedPages) {
                    Block operationBlock = page.getBlock(columnCount);
                    Block updateCaseBlock = page.getBlock(columnCount + 1);
                    List<Block> rowIdFields = null;
                    for (int pos = 0; pos < page.getPositionCount(); pos++) {
                        if (TINYINT.getByte(operationBlock, pos) != UPDATE_OPERATION_NUMBER ||
                                INTEGER.getInt(updateCaseBlock, pos) != caseNumber) {
                            continue;
                        }
                        if (rowIdFields == null) {
                            rowIdFields = RowBlock.getRowFieldsFromBlock(page.getBlock(columnCount + 2));
                        }
                        for (int i = 0; i < updateColumnList.size(); i++) {
                            setParameter(
                                    stmt,
                                    i + 1,
                                    page.getBlock(updateChannels.get(i)),
                                    pos,
                                    updateColumnTypes.get(i),
                                    updateColumnWriters.get(i));
                        }

                        int offset = updateColumnList.size();
                        for (int i = 0; i < primaryKeys.size(); i++) {
                            setParameter(
                                    stmt,
                                    offset + i + 1,
                                    rowIdFields.get(i),
                                    pos,
                                    primaryKeyTypes.get(i),
                                    primaryKeyWriters.get(i));
                        }
                        batchSize = addToBatch(stmt, batchSize);
                    }
                }
                executeRemainingBatch(stmt, batchSize);
            }
        }
    }

    private void executeInsertOperations(
            Connection connection,
            JdbcOutputTableHandle outputHandle,
            int columnCount
    ) throws SQLException {
        if (!containsOperation(columnCount, INSERT_OPERATION_NUMBER)) {
            return;
        }

        List<Type> columnTypes = outputHandle.getColumnTypes();
        List<WriteFunction> columnWriters = getColumnWriters(columnTypes);
        String insertSql = jdbcClient.buildInsertSql(outputHandle, columnWriters);
        insertSql = remoteQueryModifier.apply(session, insertSql);

        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            int batchSize = 0;
            for (Page page : bufferedPages) {
                Block operationBlock = page.getBlock(columnCount);
                for (int pos = 0; pos < page.getPositionCount(); pos++) {
                    if (TINYINT.getByte(operationBlock, pos) != INSERT_OPERATION_NUMBER) {
                        continue;
                    }
                    for (int channel = 0; channel < columnCount; channel++) {
                        setParameter(insertStmt, channel + 1, page.getBlock(channel), pos, columnTypes.get(channel), columnWriters.get(channel));
                    }
                    batchSize = addToBatch(insertStmt, batchSize);
                }
            }
            executeRemainingBatch(insertStmt, batchSize);
        }
    }

    private void validateMergeOperations(int columnCount) {
        for (Page page : bufferedPages) {
            Block operationBlock = page.getBlock(columnCount);
            Block updateCaseBlock = page.getBlock(columnCount + 1);
            for (int position = 0; position < page.getPositionCount(); position++) {
                switch (TINYINT.getByte(operationBlock, position)) {
                    case INSERT_OPERATION_NUMBER, DELETE_OPERATION_NUMBER -> {}
                    case UPDATE_OPERATION_NUMBER -> {
                        int caseNumber = INTEGER.getInt(updateCaseBlock, position);
                        Collection<ColumnHandle> updateColumns = mergeHandle.getUpdateCaseColumns().get(caseNumber);
                        if (updateColumns == null || updateColumns.isEmpty()) {
                            throw new IllegalStateException("No columns for MERGE update case " + caseNumber);
                        }
                    }
                    default -> throw new IllegalStateException();
                }
            }
        }
    }

    private boolean containsOperation(int columnCount, int expectedOperation) {
        for (Page page : bufferedPages) {
            Block operationBlock = page.getBlock(columnCount);
            for (int position = 0; position < page.getPositionCount(); position++) {
                if (TINYINT.getByte(operationBlock, position) == expectedOperation) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsUpdateCase(int columnCount, int expectedCase) {
        for (Page page : bufferedPages) {
            Block operationBlock = page.getBlock(columnCount);
            Block updateCaseBlock = page.getBlock(columnCount + 1);
            for (int position = 0; position < page.getPositionCount(); position++) {
                if (TINYINT.getByte(operationBlock, position) == UPDATE_OPERATION_NUMBER &&
                        INTEGER.getInt(updateCaseBlock, position) == expectedCase) {
                    return true;
                }
            }
        }
        return false;
    }

    private int addToBatch(PreparedStatement statement, int batchSize) throws SQLException {
        statement.addBatch();
        batchSize++;
        if (batchSize == maxBatchSize) {
            statement.executeBatch();
            return 0;
        }
        return batchSize;
    }

    private static void executeRemainingBatch(PreparedStatement statement, int batchSize) throws SQLException {
        if (batchSize > 0) {
            statement.executeBatch();
        }
    }

    private List<WriteFunction> getColumnWriters(List<Type> columnTypes) {
        return columnTypes.stream()
                .map(type -> jdbcClient.toWriteMapping(session, type).getWriteFunction())
                .toList();
    }

    private void setParameter(PreparedStatement stmt, int index, Block block, int position, Type type, WriteFunction writer) throws SQLException {
        if (block.isNull(position)) {
            writer.setNull(stmt, index);
            return;
        }

        Class<?> javaType = type.getJavaType();
        if (javaType == boolean.class) {
            ((BooleanWriteFunction) writer).set(stmt, index, type.getBoolean(block, position));
        }
        else if (javaType == long.class) {
            ((LongWriteFunction) writer).set(stmt, index, type.getLong(block, position));
        }
        else if (javaType == double.class) {
            ((DoubleWriteFunction) writer).set(stmt, index, type.getDouble(block, position));
        }
        else if (javaType == io.airlift.slice.Slice.class) {
            ((SliceWriteFunction) writer).set(stmt, index, type.getSlice(block, position));
        }
        else {
            ((ObjectWriteFunction) writer).set(stmt, index, type.getObject(block, position));
        }
    }

    private static boolean isRejectedDriverSessionAcquisition(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof RejectedExecutionException) {
                for (StackTraceElement frame : cause.getStackTrace()) {
                    if (frame.getClassName().equals("tech.ydb.table.impl.pool.SessionPool") &&
                            frame.getMethodName().equals("acquire")) {
                        // Session acquisition was rejected locally before a statement could be sent to YDB.
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void abort() {
        finished = true;
        clearBufferedPages();
    }

    private void clearBufferedPages() {
        bufferedPages.clear();
        bufferedBytes = 0;
    }
}
