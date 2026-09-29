package tech.ydb.trino;

import io.trino.testing.AbstractTestQueryFramework;
import io.trino.testing.QueryRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestYdbDefaultWriteMode extends AbstractTestQueryFramework {
    @RegisterExtension
    static final YdbHelperExtension ydb = new YdbHelperExtension();

    @Override
    protected QueryRunner createQueryRunner() throws Exception {
        return YdbQueryRunner.builder(ydb)
                .addConnectorProperty("insert.non-transactional-insert.enabled", "false")
                .useProductionClient()
                .build();
    }

    @Test
    public void testCreateTableAsSelect() {
        String tableName = "default_mode_ctas";
        String duplicateTableName = "default_mode_ctas_duplicate";
        try {
            assertUpdate("CREATE TABLE " + tableName + " (id, payload) WITH (primary_key = ARRAY['id']) " +
                    "AS VALUES (BIGINT '1', 'a'), (BIGINT '2', 'b')", 2);
            assertQuery("SELECT * FROM " + tableName,
                    "VALUES (CAST(1 AS BIGINT), 'a'), (CAST(2 AS BIGINT), 'b')");

            var tablesBefore = computeActual("SHOW TABLES").getOnlyColumnAsSet();
            assertThat(query("CREATE TABLE " + duplicateTableName + " (id, payload) " +
                    "WITH (primary_key = ARRAY['id']) AS VALUES (BIGINT '1', 'a'), (BIGINT '1', 'b')"))
                    .failure();
            assertThat(computeActual("SHOW TABLES").getOnlyColumnAsSet()).isEqualTo(tablesBefore);
            assertThat(getQueryRunner().tableExists(getSession(), duplicateTableName)).isFalse();
        }
        finally {
            assertUpdate("DROP TABLE IF EXISTS " + tableName);
            assertUpdate("DROP TABLE IF EXISTS " + duplicateTableName);
        }
    }

    @Test
    public void testInsertRequiresUnsupportedStagingTable() {
        String tableName = "default_mode_insert";
        try {
            assertUpdate("CREATE TABLE " + tableName + " (id, payload) WITH (primary_key = ARRAY['id']) " +
                    "AS VALUES (BIGINT '1', 'before')", 1);
            var tablesBefore = computeActual("SHOW TABLES").getOnlyColumnAsSet();

            assertThat(query("INSERT INTO " + tableName + " VALUES (BIGINT '2', 'after')"))
                    .failure()
                    .hasMessageMatching("(?s)Cannot call 'SCHEME_QUERY >>\\s*CREATE TABLE `tmp_trino_[^`]+` " +
                            "AS SELECT `id`, `payload` FROM `default_mode_insert` WHERE 0 = 1' " +
                            "with Status\\{code = GENERIC_ERROR\\(code=400080\\), issues = " +
                            "\\[\\d+:\\d+ - \\d+:\\d+: missing '\\(' at 'AS' \\(S_ERROR\\), " +
                            "\\d+:\\d+ - \\d+:\\d+: mismatched input 'SELECT' expecting " +
                            "\\{',', '\\)'\\} \\(S_ERROR\\)\\]\\}");

            assertQuery("SELECT * FROM " + tableName, "VALUES (CAST(1 AS BIGINT), 'before')");
            assertThat(computeActual("SHOW TABLES").getOnlyColumnAsSet()).isEqualTo(tablesBefore);
        }
        finally {
            assertUpdate("DROP TABLE IF EXISTS " + tableName);
        }
    }
}
