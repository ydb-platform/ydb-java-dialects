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
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.type.Type;
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
        AtomicInteger connections = new AtomicInteger();
        AtomicInteger executions = new AtomicInteger();
        AtomicInteger commits = new AtomicInteger();
        SQLException commitFailure = new RetryableSQLException();
        PreparedStatement statement = proxy(PreparedStatement.class, (_, method, _) -> {
            if (method.getName().equals("executeBatch")) {
                executions.incrementAndGet();
                return new int[] {1};
            }
            return null;
        });
        Connection connection = proxy(Connection.class, (_, method, _) -> switch (method.getName()) {
            case "prepareStatement" -> statement;
            case "commit" -> {
                commits.incrementAndGet();
                throw commitFailure;
            }
            default -> null;
        });
        JdbcClient jdbcClient = proxy(JdbcClient.class, (_, method, _) -> switch (method.getName()) {
            case "getConnection" -> {
                connections.incrementAndGet();
                yield connection;
            }
            case "toWriteMapping" -> WriteMapping.longMapping("Int64", PreparedStatement::setLong);
            case "buildInsertSql" -> "INSERT INTO `target` (`value`) VALUES (?)";
            default -> null;
        });
        YdbMergeSink sink = new YdbMergeSink(
                null, null, mergeHandle(), jdbcClient, () -> 1, RemoteQueryModifier.NONE, null);
        sink.storeMergedRows(new Page(
                block(BIGINT, 42), block(TINYINT, INSERT_OPERATION_NUMBER), block(INTEGER, 0), block(BIGINT, 42)));

        assertThatThrownBy(sink::finish)
                .isInstanceOfSatisfying(TrinoException.class, exception -> {
                    assertThat(exception).hasMessageContaining("commit outcome is unknown");
                    assertThat(exception.getCause()).isSameAs(commitFailure);
                });
        assertThat(connections).hasValue(1);
        assertThat(executions).hasValue(1);
        assertThat(commits).hasValue(1);
    }

    private static JdbcMergeTableHandle mergeHandle() {
        RemoteTableName tableName = new RemoteTableName(Optional.empty(), Optional.empty(), "target");
        JdbcOutputTableHandle outputHandle = new JdbcOutputTableHandle(
                tableName, List.of("value"), List.of(BIGINT), Optional.empty(), Optional.empty(), Optional.empty());
        return new JdbcMergeTableHandle(
                new JdbcTableHandle(new SchemaTableName("default", "target"), tableName, Optional.empty()),
                outputHandle,
                Map.of(),
                Optional.empty(),
                List.of(),
                List.of(),
                Map.of());
    }

    private static Block block(Type type, long value) {
        BlockBuilder builder = type.createBlockBuilder(null, 1);
        type.writeLong(builder, value);
        return builder.build();
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    private static final class RetryableSQLException extends SQLException implements YdbStatusable {
        @Override
        public Status getStatus() {
            return Status.of(StatusCode.ABORTED);
        }
    }
}
