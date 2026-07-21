package tech.ydb.trino;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.trino.Session;
import io.trino.testing.DistributedQueryRunner;
import io.trino.testing.MaterializedResult;
import io.trino.testing.QueryRunner;
import io.trino.tpch.TpchColumn;
import io.trino.tpch.TpchTable;
import org.intellij.lang.annotations.Language;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.trino.testing.TestingSession.testSessionBuilder;

public final class YdbQueryRunner {
    public static final String TPCH_SCHEMA = "ydb";
    public static final String YDB_HIDDEN_PK_COLUMN = "haha";

    private YdbQueryRunner() {}

    public static Builder builder(YdbHelperExtension ydb) {
        String jdbcUrl = buildJdbcUrl(ydb);
        return new Builder()
                .addConnectorProperty("connection-url", jdbcUrl);
    }

    private static String buildJdbcUrl(YdbHelperExtension ydb) {
        StringBuilder url = new StringBuilder("jdbc:ydb:");
        url.append(ydb.useTls() ? "grpcs://" : "grpc://");
        url.append(ydb.endpoint());
        url.append(ydb.database());
        url.append("?useQueryService=true&sessionPoolMaxSize=10");
        if (ydb.authToken() != null) {
            url.append("&token=").append(ydb.authToken());
        }
        return url.toString();
    }

    public static class Builder extends DistributedQueryRunner.Builder<Builder> {
        private final Map<String, String> connectorProperties = new HashMap<>();
        private List<TpchTable<?>> initialTables = ImmutableList.of();
        private final YdbHelperExtension ydb;

        private Builder(YdbHelperExtension ydb) {
            super(testSessionBuilder()
                    .setCatalog("ydb")
                    .setSchema(TPCH_SCHEMA)
                    .build());
            this.ydb = ydb;
        }

        public Builder addConnectorProperty(String key, String value) {
            connectorProperties.put(key, value);
            return this;
        }

        public Builder setInitialTables(List<TpchTable<?>> tables) {
            this.initialTables = tables;
            return this;
        }

        @Override
        public DistributedQueryRunner build() throws Exception {
            DistributedQueryRunner queryRunner = super.build();
            try {
                queryRunner.installPlugin(new io.trino.plugin.tpch.TpchPlugin());
                queryRunner.createCatalog("tpch", "tpch");

                queryRunner.installPlugin(new YdbPlugin(new TestingYdbJdbcModule()));
                queryRunner.createCatalog("ydb", "ydb", ImmutableMap.copyOf(connectorProperties));

                for (TpchTable<?> table : initialTables) {
                    dropTable(queryRunner, table);
                }

                for (TpchTable<?> table : initialTables) {
                    createTableWithPk(queryRunner, table);
                }

                return queryRunner;
            } catch (Throwable e) {
                queryRunner.close();
                throw e;
            }
        }

        private static void createTableWithPk(DistributedQueryRunner queryRunner, TpchTable<?> table) {
            String tableName = table.getTableName();

            String columnNames = table.getColumns().stream()
                    .map(col -> col.getColumnName().substring(2))
                    .collect(Collectors.joining(", "));

            String createSql = String.format(
                    "CREATE TABLE %s AS SELECT %s, row_number() OVER () AS %s FROM tpch.tiny.%s",
                    tableName, columnNames, YDB_HIDDEN_PK_COLUMN, tableName);

            queryRunner.execute(createSql);
        }

        private static void dropTable(DistributedQueryRunner queryRunner, TpchTable<?> table) {
            String tableName = table.getTableName();
            queryRunner.execute("DROP TABLE IF EXISTS " + tableName);
        }
    }
}
