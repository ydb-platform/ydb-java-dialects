package tech.ydb.trino;

import io.trino.plugin.jdbc.JdbcClient;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcMergeTableHandle;
import io.trino.plugin.jdbc.JdbcOutputTableHandle;
import io.trino.plugin.jdbc.JdbcTableHandle;
import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.RemoteTableName;
import io.trino.plugin.jdbc.WriteMapping;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.Page;
import io.trino.spi.TrinoException;
import io.trino.spi.block.Block;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.RowBlock;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.SchemaTableName;
import org.junit.jupiter.api.Test;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.jdbc.exception.YdbStatusable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.trino.spi.connector.ConnectorMergeSink.DELETE_OPERATION_NUMBER;
import static io.trino.spi.connector.ConnectorMergeSink.INSERT_OPERATION_NUMBER;
import static io.trino.spi.connector.ConnectorMergeSink.UPDATE_OPERATION_NUMBER;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.spi.type.TinyintType.TINYINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestYdbMergeSink {
    @Test
    void testCommitFailureIsNotRetried() {
        Calls calls = new Calls();
        SQLException commitFailure = new RetryableSQLException();
        JdbcClient jdbcClient = jdbcClient(
                calls,
                List.of(connection(statement(calls, null), calls, commitFailure)));

        assertThatThrownBy(mergeSink(jdbcClient)::finish)
                .isInstanceOfSatisfying(TrinoException.class, exception -> {
                    assertThat(exception).hasMessageContaining("commit outcome is unknown");
                    assertThat(exception.getCause()).isSameAs(commitFailure);
                });
        assertThat(calls.connections).hasValue(1);
        assertThat(calls.batchExecutions).hasValue(1);
        assertThat(calls.commits).hasValue(1);
        assertThat(calls.rollbacks).hasValue(1);
        assertThat(calls.closes).hasValue(1);
    }

    @Test
    void testPreCommitFailureRetriesOnFreshConnection() {
        Calls calls = new Calls();
        SQLException firstBatchFailure = new RetryableSQLException();
        JdbcClient jdbcClient = jdbcClient(
                calls,
                List.of(
                        connection(statement(calls, 2, firstBatchFailure), calls, null),
                        connection(statement(calls, null), calls, null)));

        mergeSink(jdbcClient, 2, insertPage(1, 2, 3)).finish();

        assertThat(calls.connections).hasValue(2);
        assertThat(calls.batchSizes).containsExactly(2, 1, 2, 1);
        assertThat(calls.commits).hasValue(1);
        assertThat(calls.rollbacks).hasValue(1);
        assertThat(calls.closes).hasValue(2);
    }

    @Test
    void testSplitsBatchWithoutIntermediateCommit() {
        Calls calls = new Calls();
        PreparedStatement statement = statement(calls, null);
        JdbcClient jdbcClient = jdbcClient(
                calls,
                List.of(connection(statement, calls, null)));

        mergeSink(jdbcClient, 2, insertPage(1, 2, 3, 4, 5)).finish();

        assertThat(calls.batchSizes).containsExactly(2, 2, 1);
        assertThat(calls.commits).hasValue(1);
        assertThat(calls.rollbacks).hasValue(0);
        assertThat(calls.closes).hasValue(1);
    }

    @Test
    void testStreamsOperationsInPhaseAndColumnOrder() {
        List<String> events = new ArrayList<>();
        StatementRecorder delete = new StatementRecorder("delete", events);
        StatementRecorder update = new StatementRecorder("update", events);
        StatementRecorder insert = new StatementRecorder("insert", events);
        List<StatementRecorder> statements = List.of(delete, update, insert);
        AtomicInteger preparedStatements = new AtomicInteger();
        Connection connection = proxy(Connection.class, (_, method, _) -> switch (method.getName()) {
            case "setAutoCommit", "close" -> null;
            case "prepareStatement" -> statements.get(preparedStatements.getAndIncrement()).statement();
            case "commit" -> {
                events.add("commit");
                yield null;
            }
            default -> null;
        });
        JdbcClient jdbcClient = proxy(JdbcClient.class, (_, method, arguments) -> switch (method.getName()) {
            case "getConnection" -> connection;
            case "quoted" -> "`" + arguments[0] + "`";
            case "toWriteMapping" -> WriteMapping.longMapping("Int64", PreparedStatement::setLong);
            case "buildInsertSql" -> "INSERT";
            default -> null;
        });

        mergeSink(jdbcClient, 10, mixedMergeHandle(), mixedPage()).finish();

        assertThat(preparedStatements).hasValue(3);
        assertThat(events).containsExactly("delete", "update", "insert", "commit");
        assertThat(delete.rows()).containsExactly(List.of(13L, 130L));
        assertThat(update.rows()).containsExactly(List.of(200L, 2000L, 12L, 120L));
        assertThat(insert.rows()).containsExactly(List.of(100L, 1L, 10L, 1000L));
    }

    @Test
    void testRejectsUnknownUpdateCaseBeforeMutation() {
        AtomicInteger preparedStatements = new AtomicInteger();
        AtomicInteger commits = new AtomicInteger();
        AtomicInteger rollbacks = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        Connection connection = proxy(Connection.class, (_, method, _) -> switch (method.getName()) {
            case "setAutoCommit" -> null;
            case "prepareStatement" -> {
                preparedStatements.incrementAndGet();
                yield null;
            }
            case "commit" -> {
                commits.incrementAndGet();
                yield null;
            }
            case "rollback" -> {
                rollbacks.incrementAndGet();
                yield null;
            }
            case "close" -> {
                closes.incrementAndGet();
                yield null;
            }
            default -> null;
        });
        JdbcClient jdbcClient = proxy(JdbcClient.class, (_, method, _) ->
                method.getName().equals("getConnection") ? connection : null);

        assertThatThrownBy(() -> mergeSink(jdbcClient, 10, mixedMergeHandle(), unknownUpdateCasePage()).finish())
                .isInstanceOfSatisfying(TrinoException.class, exception ->
                        assertThat(exception.getCause())
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("No columns for MERGE update case 99"));
        assertThat(preparedStatements).hasValue(0);
        assertThat(commits).hasValue(0);
        assertThat(rollbacks).hasValue(1);
        assertThat(closes).hasValue(1);
    }

    private static YdbMergeSink mergeSink(JdbcClient jdbcClient) {
        return mergeSink(jdbcClient, 1000, insertPage(42));
    }

    private static YdbMergeSink mergeSink(JdbcClient jdbcClient, int maxBatchSize, Page... pages) {
        return mergeSink(jdbcClient, maxBatchSize, mergeHandle(), pages);
    }

    private static YdbMergeSink mergeSink(
            JdbcClient jdbcClient,
            int maxBatchSize,
            JdbcMergeTableHandle mergeHandle,
            Page... pages) {
        ConnectorSession session = proxy(ConnectorSession.class, (_, method, _) -> {
            if (method.getName().equals("getProperty")) {
                return maxBatchSize;
            }
            return null;
        });
        YdbMergeSink sink = new YdbMergeSink(
                null,
                session,
                mergeHandle,
                jdbcClient,
                () -> 1,
                RemoteQueryModifier.NONE,
                null);
        for (Page page : pages) {
            sink.storeMergedRows(page);
        }
        return sink;
    }

    private static JdbcClient jdbcClient(Calls calls, List<Connection> connections) {
        return proxy(JdbcClient.class, (_, method, _) -> switch (method.getName()) {
            case "getConnection" -> connections.get(calls.connections.getAndIncrement());
            case "toWriteMapping" -> WriteMapping.longMapping("Int64", PreparedStatement::setLong);
            case "buildInsertSql" -> "INSERT INTO `target` (`value`) VALUES (?)";
            default -> null;
        });
    }

    private static PreparedStatement statement(Calls calls, SQLException batchFailure) {
        return statement(calls, 1, batchFailure);
    }

    private static PreparedStatement statement(Calls calls, int failureExecution, SQLException batchFailure) {
        AtomicInteger executions = new AtomicInteger();
        return proxy(PreparedStatement.class, (_, method, _) -> switch (method.getName()) {
            case "addBatch" -> {
                calls.pendingBatchRows.incrementAndGet();
                yield null;
            }
            case "executeBatch" -> {
                calls.batchExecutions.incrementAndGet();
                calls.batchSizes.add(calls.pendingBatchRows.getAndSet(0));
                if (batchFailure != null && executions.incrementAndGet() == failureExecution) {
                    throw batchFailure;
                }
                yield new int[] {1};
            }
            default -> null;
        });
    }

    private static Connection connection(PreparedStatement statement, Calls calls, SQLException commitFailure) {
        return proxy(Connection.class, (_, method, _) -> switch (method.getName()) {
            case "setAutoCommit" -> null;
            case "prepareStatement" -> statement;
            case "commit" -> {
                calls.commits.incrementAndGet();
                if (commitFailure != null) {
                    throw commitFailure;
                }
                yield null;
            }
            case "rollback" -> {
                calls.rollbacks.incrementAndGet();
                yield null;
            }
            case "close" -> {
                calls.closes.incrementAndGet();
                yield null;
            }
            default -> null;
        });
    }

    private static JdbcMergeTableHandle mergeHandle() {
        RemoteTableName remoteTableName = new RemoteTableName(Optional.empty(), Optional.empty(), "target");
        JdbcOutputTableHandle outputHandle = new JdbcOutputTableHandle(
                remoteTableName,
                List.of("value"),
                List.of(BIGINT),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        JdbcTableHandle tableHandle = new JdbcTableHandle(
                new SchemaTableName("default", "target"),
                remoteTableName,
                Optional.empty());
        return new JdbcMergeTableHandle(
                tableHandle,
                outputHandle,
                Map.of(),
                Optional.empty(),
                List.of(),
                List.of(),
                Map.of());
    }

    private static JdbcMergeTableHandle mixedMergeHandle() {
        RemoteTableName remoteTableName = new RemoteTableName(Optional.empty(), Optional.empty(), "target");
        List<JdbcColumnHandle> columns = List.of(
                column("leading"),
                column("pk1"),
                column("pk2"),
                column("value"));
        JdbcOutputTableHandle outputHandle = new JdbcOutputTableHandle(
                remoteTableName,
                columns.stream().map(JdbcColumnHandle::getColumnName).toList(),
                columns.stream().map(JdbcColumnHandle::getColumnType).toList(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        JdbcTableHandle tableHandle = new JdbcTableHandle(
                new SchemaTableName("default", "target"),
                remoteTableName,
                Optional.empty());
        return new JdbcMergeTableHandle(
                tableHandle,
                outputHandle,
                Map.of(),
                Optional.empty(),
                List.of(columns.get(1), columns.get(2)),
                columns,
                Map.of(0, List.of(columns.get(3), columns.get(0))));
    }

    private static JdbcColumnHandle column(String name) {
        return new JdbcColumnHandle(
                name,
                new JdbcTypeHandle(
                        Types.BIGINT,
                        Optional.of("Int64"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()),
                BIGINT);
    }

    private static Page insertPage(long... values) {
        Block valueBlock = bigintBlock(values);
        return new Page(
                valueBlock,
                tinyintBlock(repeat(INSERT_OPERATION_NUMBER, values.length)),
                integerBlock(new int[values.length]),
                RowBlock.fromFieldBlocks(values.length, new Block[] {valueBlock}));
    }

    private static Page mixedPage() {
        return new Page(
                bigintBlock(100, 200, 300),
                bigintBlock(1, 2, 3),
                bigintBlock(10, 20, 30),
                bigintBlock(1000, 2000, 3000),
                tinyintBlock(INSERT_OPERATION_NUMBER, UPDATE_OPERATION_NUMBER, DELETE_OPERATION_NUMBER),
                integerBlock(0, 0, 0),
                RowBlock.fromFieldBlocks(
                        3,
                        new Block[] {
                                bigintBlock(11, 12, 13),
                                bigintBlock(110, 120, 130)}));
    }

    private static Page unknownUpdateCasePage() {
        return new Page(
                bigintBlock(100),
                bigintBlock(1),
                bigintBlock(10),
                bigintBlock(1000),
                tinyintBlock(UPDATE_OPERATION_NUMBER),
                integerBlock(99),
                RowBlock.fromFieldBlocks(
                        1,
                        new Block[] {
                                bigintBlock(1),
                                bigintBlock(10)}));
    }

    private static Block bigintBlock(long... values) {
        BlockBuilder builder = BIGINT.createFixedSizeBlockBuilder(values.length);
        for (long value : values) {
            BIGINT.writeLong(builder, value);
        }
        return builder.build();
    }

    private static Block integerBlock(int... values) {
        BlockBuilder builder = INTEGER.createFixedSizeBlockBuilder(values.length);
        for (int value : values) {
            INTEGER.writeLong(builder, value);
        }
        return builder.build();
    }

    private static Block tinyintBlock(int... values) {
        BlockBuilder builder = TINYINT.createFixedSizeBlockBuilder(values.length);
        for (int value : values) {
            TINYINT.writeLong(builder, value);
        }
        return builder.build();
    }

    private static int[] repeat(int value, int count) {
        int[] values = new int[count];
        Arrays.fill(values, value);
        return values;
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    private static final class Calls {
        private final AtomicInteger connections = new AtomicInteger();
        private final AtomicInteger batchExecutions = new AtomicInteger();
        private final AtomicInteger pendingBatchRows = new AtomicInteger();
        private final AtomicInteger commits = new AtomicInteger();
        private final AtomicInteger rollbacks = new AtomicInteger();
        private final AtomicInteger closes = new AtomicInteger();
        private final List<Integer> batchSizes = new ArrayList<>();
    }

    private static final class StatementRecorder {
        private final String name;
        private final List<String> events;
        private final Map<Integer, Long> currentRow = new TreeMap<>();
        private final List<List<Long>> rows = new ArrayList<>();
        private final PreparedStatement statement;

        private StatementRecorder(String name, List<String> events) {
            this.name = name;
            this.events = events;
            this.statement = proxy(PreparedStatement.class, (_, method, arguments) -> switch (method.getName()) {
                case "setLong" -> {
                    currentRow.put((Integer) arguments[0], (Long) arguments[1]);
                    yield null;
                }
                case "addBatch" -> {
                    rows.add(List.copyOf(currentRow.values()));
                    currentRow.clear();
                    yield null;
                }
                case "executeBatch" -> {
                    events.add(name);
                    yield new int[rows.size()];
                }
                default -> null;
            });
        }

        private PreparedStatement statement() {
            return statement;
        }

        private List<List<Long>> rows() {
            return rows;
        }
    }

    private static final class RetryableSQLException extends SQLException implements YdbStatusable {
        @Override
        public Status getStatus() {
            return Status.of(StatusCode.ABORTED);
        }
    }
}
