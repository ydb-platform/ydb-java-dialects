package tech.ydb.trino;

import io.trino.plugin.jdbc.JdbcClient;
import io.trino.plugin.jdbc.JdbcMergeTableHandle;
import io.trino.plugin.jdbc.JdbcOutputTableHandle;
import io.trino.plugin.jdbc.JdbcTableHandle;
import io.trino.plugin.jdbc.RemoteTableName;
import io.trino.plugin.jdbc.WriteMapping;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.Page;
import io.trino.spi.TrinoException;
import io.trino.spi.block.Block;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.RowBlock;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static io.trino.spi.connector.ConnectorMergeSink.INSERT_OPERATION_NUMBER;
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
                        connection(statement(calls, firstBatchFailure), calls, null),
                        connection(statement(calls, null), calls, null)));

        mergeSink(jdbcClient).finish();

        assertThat(calls.connections).hasValue(2);
        assertThat(calls.batchExecutions).hasValue(2);
        assertThat(calls.commits).hasValue(1);
        assertThat(calls.rollbacks).hasValue(1);
        assertThat(calls.closes).hasValue(2);
    }

    private static YdbMergeSink mergeSink(JdbcClient jdbcClient) {
        YdbMergeSink sink = new YdbMergeSink(
                null,
                null,
                mergeHandle(),
                jdbcClient,
                () -> 1,
                RemoteQueryModifier.NONE,
                null);
        sink.storeMergedRows(insertPage());
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
        return proxy(PreparedStatement.class, (_, method, _) -> {
            if (!method.getName().equals("executeBatch")) {
                return null;
            }
            calls.batchExecutions.incrementAndGet();
            if (batchFailure != null) {
                throw batchFailure;
            }
            return new int[] {1};
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

    private static Page insertPage() {
        Block value = bigintBlock(42);
        return new Page(
                value,
                tinyintBlock(INSERT_OPERATION_NUMBER),
                integerBlock(0),
                RowBlock.fromFieldBlocks(1, new Block[] {value}));
    }

    private static Block bigintBlock(long value) {
        BlockBuilder builder = BIGINT.createFixedSizeBlockBuilder(1);
        BIGINT.writeLong(builder, value);
        return builder.build();
    }

    private static Block integerBlock(int value) {
        BlockBuilder builder = INTEGER.createFixedSizeBlockBuilder(1);
        INTEGER.writeLong(builder, value);
        return builder.build();
    }

    private static Block tinyintBlock(int value) {
        BlockBuilder builder = TINYINT.createFixedSizeBlockBuilder(1);
        TINYINT.writeLong(builder, value);
        return builder.build();
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    private static final class Calls {
        private final AtomicInteger connections = new AtomicInteger();
        private final AtomicInteger batchExecutions = new AtomicInteger();
        private final AtomicInteger commits = new AtomicInteger();
        private final AtomicInteger rollbacks = new AtomicInteger();
        private final AtomicInteger closes = new AtomicInteger();
    }

    private static final class RetryableSQLException extends SQLException implements YdbStatusable {
        @Override
        public Status getStatus() {
            return Status.of(StatusCode.ABORTED);
        }
    }
}
