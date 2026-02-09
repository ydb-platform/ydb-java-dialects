package tech.ydb.trino;

import io.trino.testing.AbstractTestQueryFramework;
import io.trino.testing.QueryRunner;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

@TestInstance(PER_CLASS)
public class YdbIntegrationTest
        extends AbstractTestQueryFramework
{
    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @Override
    protected QueryRunner createQueryRunner() throws Exception
    {
        QueryRunner runner = YdbQueryRunner.create(ydb);
        prepareTestData();
        return runner;
    }

    private void prepareTestData() throws Exception
    {
        try (Connection connection = DriverManager.getConnection(YdbQueryRunner.jdbcUrl(ydb));
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS test_table");
            statement.execute("CREATE TABLE test_table (" +
                    "id Uint64 NOT NULL, " +
                    "name Utf8, " +
                    "created_at Timestamp, " +
                    "PRIMARY KEY (id)" +
                    ");");
            statement.execute("UPSERT INTO test_table (id, name, created_at) VALUES (1, 'alice', CurrentUtcTimestamp())");
            statement.execute("UPSERT INTO test_table (id, name, created_at) VALUES (2, 'bob', CurrentUtcTimestamp())");
        }
    }

    @Test
    public void testSelectAndPredicate()
    {
        assertQuery("SELECT name FROM test_table WHERE id = 1", "VALUES 'alice'");
    }

    @Test
    public void testAggregation()
    {
        assertQuery("SELECT count(*), sum(id) FROM test_table", "VALUES (2, 3)");
    }
}
