package tech.ydb.trino;

import io.trino.testing.BaseConnectorSmokeTest;
import io.trino.testing.QueryRunner;
import io.trino.testing.TestingConnectorBehavior;
import io.trino.testing.sql.TestTable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestYdbConnectorSmokeTest extends BaseConnectorSmokeTest {
    @RegisterExtension
    static final YdbHelperExtension ydb = new YdbHelperExtension();

    @Override
    protected QueryRunner createQueryRunner() throws Exception {
        return YdbQueryRunner.builder(ydb)
                .setInitialTables(REQUIRED_TPCH_TABLES)
                .build();
    }

    @Override
    protected boolean hasBehavior(TestingConnectorBehavior connectorBehavior) {
        return switch (connectorBehavior) {
            case SUPPORTS_CREATE_VIEW,
                 SUPPORTS_CREATE_SCHEMA,
                 SUPPORTS_RENAME_SCHEMA,
                 SUPPORTS_SET_COLUMN_TYPE,
                 SUPPORTS_ROW_TYPE,
                 SUPPORTS_RENAME_COLUMN,
                 SUPPORTS_TRUNCATE,
                 SUPPORTS_COMMENT_ON_COLUMN,
                 SUPPORTS_COMMENT_ON_TABLE,
                 SUPPORTS_DROP_SCHEMA_CASCADE,
                 SUPPORTS_CREATE_MATERIALIZED_VIEW,
                 SUPPORTS_CREATE_TABLE_WITH_COLUMN_COMMENT,
                 SUPPORTS_CREATE_TABLE_WITH_TABLE_COMMENT,
                 SUPPORTS_ADD_COLUMN_WITH_COMMENT,
                 SUPPORTS_ADD_COLUMN_WITH_POSITION,
                 SUPPORTS_CREATE_FEDERATED_MATERIALIZED_VIEW,
                 SUPPORTS_RENAME_TABLE_ACROSS_SCHEMAS,
                 SUPPORTS_ARRAY,
                 SUPPORTS_MAP_TYPE,
                 SUPPORTS_DEFAULT_COLUMN_VALUE,
                 SUPPORTS_SET_DEFAULT_COLUMN_VALUE,
                 SUPPORTS_DROP_DEFAULT_COLUMN_VALUE,
                 SUPPORTS_ADD_COLUMN_NOT_NULL_CONSTRAINT -> false;
            case SUPPORTS_TOPN_PUSHDOWN_WITH_VARCHAR -> true;
            default -> super.hasBehavior(connectorBehavior);
        };
    }

    @Test
    public void testVarbinaryRoundTrip() {
        try (TestTable table = newTrinoTable("binary_roundtrip", "(id bigint, payload varbinary)")) {
            assertUpdate("INSERT INTO " + table.getName() + " (id, payload) VALUES " +
                    "(1, X''), (2, X'00008081FF'), (3, X'EFBFBD'), (4, NULL)", 4);
            assertThat(query("SELECT id, payload FROM " + table.getName()))
                    .matches("VALUES (BIGINT '1', X''), (BIGINT '2', X'00008081FF'), " +
                            "(BIGINT '3', X'EFBFBD'), (BIGINT '4', CAST(NULL AS varbinary))");
        }
    }

    @Test
    @Override
    public void testShowCreateTable() {
        String catalog = getSession().getCatalog().orElseThrow();
        String schema = getSession().getSchema().orElseThrow();
        Assertions.assertThat(computeScalar("SHOW CREATE TABLE region")).isEqualTo(String.format(
                "CREATE TABLE %s.%s.region (\n" +
                        "   regionkey bigint,\n" +
                        "   name varchar,\n" +
                        "   comment varchar\n" +
                        ")",
                catalog,
                schema));
    }
}
