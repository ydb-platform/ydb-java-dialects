package tech.ydb.trino;

import io.trino.Session;
import io.trino.testing.DistributedQueryRunner;
import io.trino.testing.QueryRunner;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.util.Map;

import static io.trino.testing.TestingSession.testSessionBuilder;

public final class YdbQueryRunner
{
    private YdbQueryRunner() {}

    public static QueryRunner create(YdbHelperExtension ydb)
            throws Exception
    {
        Session session = testSessionBuilder().setCatalog("ydb").setSchema("ydb").build();

        DistributedQueryRunner queryRunner = DistributedQueryRunner.builder(session).build();
        queryRunner.installPlugin(new YdbPlugin());
        queryRunner.createCatalog("ydb", "ydb", Map.of(
                "connection-url", jdbcUrl(ydb)));

        return queryRunner;
    }

    public static String jdbcUrl(YdbHelperExtension ydb)
    {
        return "jdbc:ydb:" + (ydb.useTls() ? "grpcs://" : "grpc://") + ydb.endpoint() + ydb.database();
    }
}
