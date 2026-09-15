package tech.ydb.trino;

import io.trino.testing.AbstractTestQueryFramework;
import io.trino.testing.QueryRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestYdbCreateTable extends AbstractTestQueryFramework {
    @RegisterExtension
    static final YdbHelperExtension ydb = new YdbHelperExtension();

    @Override
    protected QueryRunner createQueryRunner() throws Exception {
        return YdbQueryRunner.builder(ydb)
                .useProductionClient()
                .build();
    }

    @Test
    public void testCreateTableWithPrimaryKey() {
        String tableName = "production_create_with_pk";
        try {
            assertUpdate("CREATE TABLE " + tableName + " (tenant bigint, event_id bigint, payload varchar) " +
                    "WITH (primary_key = ARRAY['event_id', 'tenant'])");
            assertUpdate("INSERT INTO " + tableName + " VALUES (1, 10, 'a'), (2, NULL, 'b')", 2);
            assertQuery("SELECT * FROM " + tableName, "VALUES (1, 10, 'a'), (2, NULL, 'b')");
            assertThat((String) computeScalar("SHOW CREATE TABLE " + tableName))
                    .contains("primary_key = ARRAY['event_id', 'tenant']");
        }
        finally {
            assertUpdate("DROP TABLE IF EXISTS " + tableName);
        }
    }

    @Test
    public void testCreateTableAsSelectWithPrimaryKey() {
        String tableName = "production_ctas_with_pk";
        String duplicateTableName = "production_ctas_duplicate_pk";
        try {
            assertUpdate("CREATE TABLE " + tableName + " (id, payload) WITH (primary_key = ARRAY['id']) " +
                    "AS VALUES (BIGINT '1', 'a'), (BIGINT '2', 'b')", 2);
            assertQuery("SELECT * FROM " + tableName, "VALUES (BIGINT '1', 'a'), (BIGINT '2', 'b')");

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
    public void testInvalidPrimaryKeyProperties() {
        String tableName = "production_create_invalid_pk";
        assertQueryFails("CREATE TABLE " + tableName + " (id bigint)",
                ".*Table property 'primary_key' must contain at least one column");
        assertQueryFails("CREATE TABLE " + tableName + " (id bigint) " +
                        "WITH (primary_key = CAST(ARRAY[] AS ARRAY(VARCHAR)))",
                ".*Table property 'primary_key' must contain at least one column");
        assertQueryFails("CREATE TABLE " + tableName + " (id bigint) WITH (primary_key = ARRAY['missing'])",
                ".*Column 'missing' specified in table property 'primary_key' does not exist");
        assertQueryFails("CREATE TABLE " + tableName + " (id bigint) WITH (primary_key = ARRAY['id', 'id'])",
                ".*Table property 'primary_key' contains duplicate columns");
        assertQueryFails("CREATE TABLE " + tableName + " (id bigint) WITH (primary_key = ARRAY[CAST(NULL AS VARCHAR)])",
                ".*Table property 'primary_key' must not contain null columns");
        assertThat(getQueryRunner().tableExists(getSession(), tableName)).isFalse();
    }
}
