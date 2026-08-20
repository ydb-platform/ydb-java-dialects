package tech.ydb.trino;

import io.trino.plugin.base.mapping.IdentifierMapping;
import io.trino.plugin.jdbc.BaseJdbcConfig;
import io.trino.plugin.jdbc.ConnectionFactory;
import io.trino.plugin.jdbc.JdbcColumnHandle;
import io.trino.plugin.jdbc.JdbcTypeHandle;
import io.trino.plugin.jdbc.QueryBuilder;
import io.trino.plugin.jdbc.RemoteTableName;
import io.trino.plugin.jdbc.logging.RemoteQueryModifier;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.SchemaTableName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.spi.type.VarcharType.VARCHAR;
import static org.assertj.core.api.Assertions.assertThat;

class TestYdbInsertStaging {
    @Test
    void testCreatesStagingTableWithSerialKeyAndRemoteColumnTypes() {
        List<JdbcColumnHandle> columns = List.of(
                column("note", Types.VARCHAR, "Text", VARCHAR, true),
                column("pk", Types.BIGINT, "Uint64", BIGINT, false),
                column("_trino_staging_id", Types.INTEGER, "Int32", INTEGER, true));
        TestingYdbClient client = new TestingYdbClient(columns);

        client.copyTableSchema(
                null,
                null,
                null,
                null,
                "dir/target",
                "dir/staging",
                List.of("pk", "note", "_trino_staging_id"));

        assertThat(client.executedSql()).isEqualTo(
                "CREATE TABLE `dir/staging` (`_trino_staging_id_1` BigSerial, " +
                        "`pk` Uint64 NOT NULL, `note` Text, `_trino_staging_id` Int32, " +
                        "PRIMARY KEY (`_trino_staging_id_1`))");
    }

    private static JdbcColumnHandle column(
            String name,
            int jdbcType,
            String remoteType,
            io.trino.spi.type.Type trinoType,
            boolean nullable) {
        return JdbcColumnHandle.builder()
                .setColumnName(name)
                .setJdbcTypeHandle(new JdbcTypeHandle(
                        jdbcType,
                        Optional.of(remoteType),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()))
                .setColumnType(trinoType)
                .setNullable(nullable)
                .setComment(Optional.empty())
                .build();
    }

    private static final class TestingYdbClient extends YdbClient {
        private final List<JdbcColumnHandle> columns;
        private String executedSql;

        private TestingYdbClient(List<JdbcColumnHandle> columns) {
            super(
                    new BaseJdbcConfig(),
                    proxy(ConnectionFactory.class),
                    proxy(QueryBuilder.class),
                    proxy(IdentifierMapping.class),
                    RemoteQueryModifier.NONE);
            this.columns = columns;
        }

        @Override
        public List<JdbcColumnHandle> getColumns(
                ConnectorSession session,
                SchemaTableName schemaTableName,
                RemoteTableName remoteTableName) {
            return columns;
        }

        @Override
        protected void execute(ConnectorSession session, Connection connection, String query) throws SQLException {
            executedSql = query;
        }

        private String executedSql() {
            return executedSql;
        }
    }

    private static <T> T proxy(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (_, _, _) -> null));
    }
}
