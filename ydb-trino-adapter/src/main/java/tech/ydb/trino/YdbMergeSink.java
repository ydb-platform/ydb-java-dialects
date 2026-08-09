package tech.ydb.trino;

import com.google.common.collect.ImmutableList;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.trino.plugin.jdbc.*;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.Page;
import io.trino.spi.TrinoException;
import io.trino.spi.block.Block;
import io.trino.spi.block.RowBlock;
import io.trino.spi.connector.*;
import io.trino.spi.type.Type;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.trino.plugin.jdbc.JdbcErrorCode.JDBC_ERROR;
import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.spi.type.TinyintType.TINYINT;
import static java.util.concurrent.CompletableFuture.completedFuture;

/*
  Code partly borrowed from JdbcMergeSink. Key differences are:
  - We do not write into the target table on each storeMergedRows call; instead, we store the necessary pages
    and write everything at once when finish() is called. Without it, I had serious atomicity problems and flaky tests.
  - We retry in case YDB returns a retryable exception on an update attempt.
  - We do not use temporary tables unlike base Trino implementation, but write straight to YDB target.
 */
public class YdbMergeSink implements ConnectorMergeSink {
    private final ConnectorSession session;
    private final JdbcMergeTableHandle mergeHandle;
    private final JdbcClient jdbcClient;
    private final ConnectorPageSinkId pageSinkId;
    private final RemoteQueryModifier remoteQueryModifier;

    private final List<Page> bufferedPages = new ArrayList<>();
    private boolean finished = false;

    public YdbMergeSink(
            @SuppressWarnings("unused") ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorMergeTableHandle mergeTableHandle,
            JdbcClient jdbcClient,
            ConnectorPageSinkId pageSinkId,
            RemoteQueryModifier remoteQueryModifier,
            @SuppressWarnings("unused") QueryBuilder queryBuilder
    ) {
        this.session = session;
        this.mergeHandle = (JdbcMergeTableHandle) mergeTableHandle;
        this.jdbcClient = jdbcClient;
        this.pageSinkId = pageSinkId;
        this.remoteQueryModifier = remoteQueryModifier;
    }

    @Override
    public void storeMergedRows(Page page) {
        if (finished) {
            throw new IllegalStateException();
        }
        bufferedPages.add(page);
    }

    @Override
    public CompletableFuture<Collection<Slice>> finish() {
        finished = true;

        int maxAttempts = 10;
        Exception lastException = null;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Connection connection = null;
            try {
                connection = openConnection();
                executeMergeInTransaction(connection);
                connection.commit();

                Slice value = Slices.allocate(Long.BYTES);
                value.setLong(0, pageSinkId.getId());
                return completedFuture(ImmutableList.of(value));
            }
            catch (Exception e) {
                if (connection != null) {
                    try {
                        connection.rollback();
                    }
                    catch (SQLException rollbackError) {
                        e.addSuppressed(rollbackError);
                    }
                    finally {
                        try {
                            connection.close();
                        }
                        catch (SQLException closeError) {
                            e.addSuppressed(closeError);
                        }
                    }
                }

                if (!isRetryableError(e)) {
                    throw new TrinoException(JDBC_ERROR, e);
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

    private Connection openConnection() throws SQLException {
        JdbcOutputTableHandle outputHandle = mergeHandle.getOutputTableHandle();
        Connection connection = jdbcClient.getConnection(session, outputHandle);
        connection.setAutoCommit(false);
        return connection;
    }

    private void executeMergeInTransaction(Connection connection) throws SQLException {
        JdbcOutputTableHandle outputHandle = mergeHandle.getOutputTableHandle();
        List<JdbcColumnHandle> primaryKeys = mergeHandle.getPrimaryKeys();

        int columnCount = outputHandle.getColumnNames().size();
        List<JdbcColumnHandle> columns = mergeHandle.getDataColumns();

        List<Page> insertPages = new ArrayList<>();
        List<Page> deletePages = new ArrayList<>();
        Map<Integer, List<Page>> updatePagesByCase = new HashMap<>();

        for (Page page : bufferedPages) {
            separateMergeOperations(page, columnCount, insertPages, deletePages, updatePagesByCase, columns);
        }

        if (!deletePages.isEmpty()) {
            executeDeleteOperations(connection, deletePages, primaryKeys);
        }

        if (!updatePagesByCase.isEmpty()) {
            executeUpdateOperations(connection, updatePagesByCase, primaryKeys, columns);
        }

        if (!insertPages.isEmpty()) {
            executeInsertOperations(connection, insertPages, outputHandle);
        }
    }

    private void executeDeleteOperations(
            Connection connection,
            List<Page> deletePages,
            List<JdbcColumnHandle> primaryKeys
    ) throws SQLException {
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

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Page page : deletePages) {
                for (int pos = 0; pos < page.getPositionCount(); pos++) {
                    for (int i = 0; i < primaryKeys.size(); i++) {
                        Block block = page.getBlock(i);
                        Type type = primaryKeys.get(i).getColumnType();
                        WriteFunction writer = jdbcClient.toWriteMapping(session, type).getWriteFunction();
                        setParameter(stmt, i + 1, block, pos, type, writer);
                    }
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
        }
    }

    private void executeUpdateOperations(
            Connection connection,
            Map<Integer, List<Page>> updatePagesByCase,
            List<JdbcColumnHandle> primaryKeys,
            List<JdbcColumnHandle> columns
    ) throws SQLException {
        String tableName = mergeHandle.getTableHandle().getRequiredNamedRelation().getRemoteTableName().getTableName();

        for (Map.Entry<Integer, List<Page>> entry : updatePagesByCase.entrySet()) {
            int caseNumber = entry.getKey();
            List<Page> pages = entry.getValue();

            Collection<ColumnHandle> updateColumns = mergeHandle.getUpdateCaseColumns().get(caseNumber);
            if (updateColumns == null || updateColumns.isEmpty()) {
                continue;
            }

            StringBuilder updateSql = new StringBuilder("UPDATE ");
            updateSql.append(jdbcClient.quoted(tableName));
            updateSql.append(" SET ");

            Set<Integer> updateChannelsSet = updateColumns.stream()
                    .map(JdbcColumnHandle.class::cast)
                    .map(columns::indexOf)
                    .collect(toImmutableSet());

            List<JdbcColumnHandle> updateColumnList = new ArrayList<>();
            for (int channel = 0; channel < columns.size(); channel++) {
                if (updateChannelsSet.contains(channel)) {
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

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (Page page : pages) {
                    for (int pos = 0; pos < page.getPositionCount(); pos++) {
                        for (int i = 0; i < updateColumnList.size(); i++) {
                            Block block = page.getBlock(i);
                            Type type = updateColumnList.get(i).getColumnType();
                            WriteFunction writer = jdbcClient.toWriteMapping(session, type).getWriteFunction();
                            setParameter(stmt, i + 1, block, pos, type, writer);
                        }

                        int offset = updateColumnList.size();
                        for (int i = 0; i < primaryKeys.size(); i++) {
                            Block block = page.getBlock(offset + i);
                            Type type = primaryKeys.get(i).getColumnType();
                            WriteFunction writer = jdbcClient.toWriteMapping(session, type).getWriteFunction();
                            setParameter(stmt, offset + i + 1, block, pos, type, writer);
                        }
                        stmt.addBatch();
                    }
                }
                stmt.executeBatch();
            }
        }
    }

    private void executeInsertOperations(
            Connection connection,
            List<Page> insertPages,
            JdbcOutputTableHandle outputHandle
    ) throws SQLException {
        List<Type> columnTypes = outputHandle.getColumnTypes();
        List<WriteFunction> columnWriters = getColumnWriters(columnTypes);
        String insertSql = jdbcClient.buildInsertSql(outputHandle, columnWriters);
        insertSql = remoteQueryModifier.apply(session, insertSql);

        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            int columnCount = outputHandle.getColumnNames().size();

            for (Page page : insertPages) {
                for (int pos = 0; pos < page.getPositionCount(); pos++) {
                    for (int channel = 0; channel < columnCount; channel++) {
                        setParameter(insertStmt, channel + 1, page.getBlock(channel), pos, columnTypes.get(channel), columnWriters.get(channel));
                    }
                    insertStmt.addBatch();
                }
            }
            insertStmt.executeBatch();
        }
    }

    private void separateMergeOperations(
            Page page,
            int columnCount,
            List<Page> insertPages,
            List<Page> deletePages,
            Map<Integer, List<Page>> updatePagesByCase,
            List<JdbcColumnHandle> columns
    ) {
        Block operationBlock = page.getBlock(columnCount);
        Block updateCaseBlock = page.getBlock(columnCount + 1);

        List<Integer> insertPositions = new ArrayList<>();
        List<Integer> deletePositions = new ArrayList<>();
        Map<Integer, List<Integer>> updatePositionsByCase = new HashMap<>();

        for (int pos = 0; pos < page.getPositionCount(); pos++) {
            int operation = TINYINT.getByte(operationBlock, pos);
            switch (operation) {
                case INSERT_OPERATION_NUMBER -> insertPositions.add(pos);
                case DELETE_OPERATION_NUMBER -> deletePositions.add(pos);
                case UPDATE_OPERATION_NUMBER -> {
                    int caseNumber = INTEGER.getInt(updateCaseBlock, pos);
                    updatePositionsByCase.computeIfAbsent(caseNumber, _ -> new ArrayList<>()).add(pos);
                }
                default -> throw new IllegalStateException();
            }
        }

        if (!insertPositions.isEmpty()) {
            int[] positions = insertPositions.stream().mapToInt(Integer::intValue).toArray();
            Page insertData = page.getColumns(IntStream.range(0, columnCount).toArray())
                    .getPositions(positions, 0, positions.length);
            insertPages.add(insertData);
        }

        if (!deletePositions.isEmpty()) {
            int[] positions = deletePositions.stream().mapToInt(Integer::intValue).toArray();
            Block rowIdBlock = page.getBlock(columnCount + 2);
            List<Block> rowIdFields = RowBlock.getRowFieldsFromBlock(rowIdBlock);
            Block[] deleteBlocks = new Block[rowIdFields.size()];
            for (int i = 0; i < rowIdFields.size(); i++) {
                deleteBlocks[i] = rowIdFields.get(i).getPositions(positions, 0, positions.length);
            }
            deletePages.add(new Page(positions.length, deleteBlocks));
        }

        for (Map.Entry<Integer, List<Integer>> entry : updatePositionsByCase.entrySet()) {
            int caseNumber = entry.getKey();
            int[] positions = entry.getValue().stream().mapToInt(Integer::intValue).toArray();

            Collection<ColumnHandle> updateColumns = mergeHandle.getUpdateCaseColumns().get(caseNumber);
            if (updateColumns == null) continue;

            Set<Integer> updateChannelsSet = updateColumns.stream()
                    .map(JdbcColumnHandle.class::cast)
                    .map(columns::indexOf)
                    .collect(toImmutableSet());

            List<Integer> updateChannelsList = new ArrayList<>();
            for (int channel = 0; channel < columns.size(); channel++) {
                if (updateChannelsSet.contains(channel)) {
                    updateChannelsList.add(channel);
                }
            }

            int[] updateChannels = updateChannelsList.stream().mapToInt(Integer::intValue).toArray();

            Block rowIdBlock = page.getBlock(columnCount + 2);
            List<Block> rowIdFields = RowBlock.getRowFieldsFromBlock(rowIdBlock);
            Block[] updateBlocks = new Block[updateChannels.length + rowIdFields.size()];

            int blockIdx = 0;
            for (int channel : updateChannels) {
                updateBlocks[blockIdx++] = page.getBlock(channel).getPositions(positions, 0, positions.length);
            }
            for (Block rowIdField : rowIdFields) {
                updateBlocks[blockIdx++] = rowIdField.getPositions(positions, 0, positions.length);
            }

            updatePagesByCase.computeIfAbsent(caseNumber, _ -> new ArrayList<>())
                    .add(new Page(positions.length, updateBlocks));
        }
    }

    private List<WriteFunction> getColumnWriters(List<Type> columnTypes) {
        return columnTypes.stream()
                .map(type -> jdbcClient.toWriteMapping(session, type).getWriteFunction())
                .collect(java.util.stream.Collectors.toList());
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