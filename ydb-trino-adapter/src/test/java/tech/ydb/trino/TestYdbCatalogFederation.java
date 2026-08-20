package tech.ydb.trino;

import io.trino.Session;
import io.trino.testing.AbstractTestQueryFramework;
import io.trino.testing.DistributedQueryRunner;
import io.trino.testing.QueryRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.testcontainers.DockerClientFactory;
import tech.ydb.test.integration.YdbEnvironment;
import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.docker.DockerHelperFactory;
import tech.ydb.test.integration.docker.YdbDockerContainer;
import tech.ydb.test.integration.utils.PortsGenerator;

import static io.trino.testing.TestingNames.randomNameSuffix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@Execution(SAME_THREAD)
public class TestYdbCatalogFederation
        extends AbstractTestQueryFramework
{
    private static final String PRIMARY_CATALOG = "ydb";
    private static final String SECONDARY_CATALOG = "ydb_analytics";

    private YdbHelper primaryYdb;
    private YdbHelper secondaryYdb;

    private static final class LocalDockerEnvironment
            extends YdbEnvironment
    {
        @Override
        public String ydbEndpoint()
        {
            return null;
        }

        @Override
        public String ydbDatabase()
        {
            return null;
        }

        @Override
        public String dockerDatabase()
        {
            return "/local";
        }

        @Override
        public boolean dockerReuse()
        {
            return false;
        }
    }

    private static YdbHelper startLocalYdb()
    {
        LocalDockerEnvironment environment = new LocalDockerEnvironment();
        assumeFalse(environment.disableIntegrationTests(), "YDB integration tests are disabled");
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker-backed YDB is unavailable");

        YdbDockerContainer container = new YdbDockerContainer(environment, new PortsGenerator());
        try {
            return new DockerHelperFactory(environment, container).createHelper();
        }
        catch (Exception | Error failure) {
            closeSuppressing(failure, container);
            throw failure;
        }
    }

    @Override
    protected QueryRunner createQueryRunner()
            throws Exception
    {
        DistributedQueryRunner queryRunner = null;
        primaryYdb = startLocalYdb();
        try {
            secondaryYdb = startLocalYdb();
            queryRunner = YdbQueryRunner.builder(primaryYdb)
                    .setWorkerCount(1)
                    .build();
            queryRunner.createCatalog(
                    SECONDARY_CATALOG,
                    "ydb",
                    YdbQueryRunner.connectorProperties(secondaryYdb));

            closeAfterClass(primaryYdb);
            closeAfterClass(secondaryYdb);
            return queryRunner;
        }
        catch (Exception | Error failure) {
            closeSuppressing(failure, queryRunner);
            closeSuppressing(failure, secondaryYdb);
            closeSuppressing(failure, primaryYdb);
            throw failure;
        }
    }

    private static void closeSuppressing(Throwable failure, AutoCloseable resource)
    {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        }
        catch (Throwable closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private static String tableName(String catalog, String table)
    {
        return "%s.%s.%s".formatted(catalog, YdbQueryRunner.DEFAULT_SCHEMA, table);
    }

    private AutoCloseable dropTableOnClose(String table)
    {
        return () -> assertQuerySucceeds("DROP TABLE IF EXISTS " + table);
    }

    private void createMarkerTable(String table, String marker)
    {
        assertUpdate("CREATE TABLE " + table + " (id bigint NOT NULL, marker varchar)");
        assertUpdate("INSERT INTO " + table + " (id, marker) VALUES (1, '" + marker + "')", 1);
    }

    @Test
    public void testCatalogDiscoveryAndUseDefaultSchema()
            throws Exception
    {
        assertThat(primaryYdb.database()).isEqualTo("/local");
        assertThat(secondaryYdb.database()).isEqualTo("/local");
        assertThat(primaryYdb.endpoint()).isNotEqualTo(secondaryYdb.endpoint());
        assertThat(computeActual("SHOW CATALOGS").getOnlyColumnAsSet())
                .contains(PRIMARY_CATALOG, SECONDARY_CATALOG);

        assertQuerySucceeds("USE ydb.default");
        assertQuerySucceeds("USE ydb_analytics.default");
        assertQueryFails("USE ydb_analytics.missing", ".*Schema does not exist: ydb_analytics.missing.*");

        String table = "federation_use_" + randomNameSuffix();
        String analyticsTable = tableName(SECONDARY_CATALOG, table);
        try (AutoCloseable ignored = dropTableOnClose(analyticsTable)) {
            createMarkerTable(analyticsTable, "analytics");

            Session analyticsSession = Session.builder(getSession())
                    .setCatalog(SECONDARY_CATALOG)
                    .setSchema(YdbQueryRunner.DEFAULT_SCHEMA)
                    .build();
            assertQuery(analyticsSession, "SELECT marker FROM " + table, "VALUES 'analytics'");
        }
    }

    @Test
    public void testSameNamedTablesAreIsolated()
            throws Exception
    {
        String table = "federation_isolation_" + randomNameSuffix();
        String primaryTable = tableName(PRIMARY_CATALOG, table);
        String analyticsTable = tableName(SECONDARY_CATALOG, table);
        try (AutoCloseable ignoredPrimary = dropTableOnClose(primaryTable);
                AutoCloseable ignoredAnalytics = dropTableOnClose(analyticsTable)) {
            createMarkerTable(primaryTable, "primary");
            createMarkerTable(analyticsTable, "analytics");

            assertQuery("SELECT id, marker FROM " + primaryTable, "VALUES (CAST(1 AS BIGINT), 'primary')");
            assertQuery("SELECT id, marker FROM " + analyticsTable, "VALUES (CAST(1 AS BIGINT), 'analytics')");
        }
    }

    @Test
    public void testCrossCatalogReadJoin()
            throws Exception
    {
        String primaryTable = tableName(PRIMARY_CATALOG, "federation_primary_" + randomNameSuffix());
        String analyticsTable = tableName(SECONDARY_CATALOG, "federation_analytics_" + randomNameSuffix());
        try (AutoCloseable ignoredPrimary = dropTableOnClose(primaryTable);
                AutoCloseable ignoredAnalytics = dropTableOnClose(analyticsTable)) {
            createMarkerTable(primaryTable, "primary");
            createMarkerTable(analyticsTable, "analytics");

            assertQuery(
                    "SELECT p.id, p.marker, a.marker " +
                            "FROM " + primaryTable + " p " +
                            "JOIN " + analyticsTable + " a ON p.id = a.id",
                    "VALUES (CAST(1 AS BIGINT), 'primary', 'analytics')");
        }
    }

    @Test
    public void testAutocommitReadManyWriteOne()
            throws Exception
    {
        String primarySource = tableName(PRIMARY_CATALOG, "federation_primary_" + randomNameSuffix());
        String analyticsSource = tableName(SECONDARY_CATALOG, "federation_analytics_" + randomNameSuffix());
        String primaryTarget = tableName(PRIMARY_CATALOG, "federation_output_" + randomNameSuffix());
        try (AutoCloseable ignoredPrimarySource = dropTableOnClose(primarySource);
                AutoCloseable ignoredAnalyticsSource = dropTableOnClose(analyticsSource);
                AutoCloseable ignoredPrimaryTarget = dropTableOnClose(primaryTarget)) {
            createMarkerTable(primarySource, "primary");
            createMarkerTable(analyticsSource, "analytics");
            assertUpdate("CREATE TABLE " + primaryTarget + " (id bigint NOT NULL, marker varchar)");

            assertUpdate(
                    "INSERT INTO " + primaryTarget + " (id, marker) " +
                            "SELECT p.id, p.marker || ':' || a.marker " +
                            "FROM " + primarySource + " p " +
                            "JOIN " + analyticsSource + " a ON p.id = a.id",
                    1);
            assertQuery("SELECT id, marker FROM " + primaryTarget, "VALUES (CAST(1 AS BIGINT), 'primary:analytics')");
        }
    }

    @Test
    public void testExplicitTransactionReadsAndRejectsYdbWrite()
            throws Exception
    {
        String primarySource = tableName(PRIMARY_CATALOG, "federation_primary_" + randomNameSuffix());
        String analyticsSource = tableName(SECONDARY_CATALOG, "federation_analytics_" + randomNameSuffix());
        String analyticsTarget = tableName(SECONDARY_CATALOG, "federation_output_" + randomNameSuffix());
        try (AutoCloseable ignoredPrimarySource = dropTableOnClose(primarySource);
                AutoCloseable ignoredAnalyticsSource = dropTableOnClose(analyticsSource);
                AutoCloseable ignoredAnalyticsTarget = dropTableOnClose(analyticsTarget)) {
            createMarkerTable(primarySource, "primary");
            createMarkerTable(analyticsSource, "analytics");
            assertUpdate("CREATE TABLE " + analyticsTarget + " (id bigint NOT NULL, marker varchar)");

            String crossCatalogJoin = "SELECT p.id, p.marker, a.marker " +
                    "FROM " + primarySource + " p " +
                    "JOIN " + analyticsSource + " a ON p.id = a.id";
            Session transactionBase = Session.builder(getSession())
                    .setCatalog(PRIMARY_CATALOG)
                    .setSchema(YdbQueryRunner.DEFAULT_SCHEMA)
                    .build();

            assertThatThrownBy(() -> newTransaction().execute(transactionBase, transactionSession -> {
                assertQuery(
                        transactionSession,
                        crossCatalogJoin,
                        "VALUES (CAST(1 AS BIGINT), 'primary', 'analytics')");
                assertUpdate(
                        transactionSession,
                        "INSERT INTO " + analyticsTarget + " (id, marker) VALUES (2, 'must-not-commit')",
                        1);
            }))
                    .hasMessageMatching("Catalog only supports writes using autocommit: ydb_analytics");

            assertQuery("SELECT count(*) FROM " + analyticsTarget, "VALUES CAST(0 AS BIGINT)");
        }
    }
}
