package tech.ydb.flyway.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

/**
 * @author Kirill Kurdyukov
 */
public class YdbFlywayTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    private static final String[] EVOLUTION_SCHEMA_MIGRATION_DIRS = new String[]{
            "migration-step-1", "migration-step-2",
            "migration-step-3", "migration-step-4",
            "migration-step-5", "migration",
    };
    private static final HashSet<String> EXPECTED_SCRIPTS = new HashSet<>();

    static {
        EXPECTED_SCRIPTS.add("V1__create_series.sql");
        EXPECTED_SCRIPTS.add("V2__create_seasons.sql");
        EXPECTED_SCRIPTS.add("V3__create_episodes.sql");
        EXPECTED_SCRIPTS.add("V4__load_data.sql");
        EXPECTED_SCRIPTS.add("V5__create_series_title_index.sql");
        EXPECTED_SCRIPTS.add("V6__rename_index_title_index.sql");
    }

    @Test
    void simpleTest() {
        assertTrue(createFlyway("classpath:db/migration").migrate().success);

        verifyTest();
    }

    @Test
    void schemaHistoryStateTest() {
        for (String migrationDir : EVOLUTION_SCHEMA_MIGRATION_DIRS) {
            assertTrue(createFlyway("classpath:db/" + migrationDir).migrate().success);
        }

        verifyTest();
    }

    @Test
    void schemaHistoryConcurrencyUpdateTest() throws ExecutionException, InterruptedException {
        int threadPoolSize = 10;

        ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);

        for (int migrationStep = 0; migrationStep < EVOLUTION_SCHEMA_MIGRATION_DIRS.length; migrationStep++) {
            List<Future<?>> taskFutures = new ArrayList<>();

            for (int i = 0; i < threadPoolSize * 2; i++) {
                int finalMigrationStep = migrationStep;

                taskFutures.add(
                        threadPool.submit(() -> assertTrue(
                                createFlyway("classpath:db/" + EVOLUTION_SCHEMA_MIGRATION_DIRS[finalMigrationStep])
                                        .migrate().success
                        ))
                );
            }

            for (Future<?> taskFuture : taskFutures) {
                taskFuture.get();
            }
        }

        verifyTest();
    }

    @Test
        // https://documentation.red-gate.com/flyway/flyway-cli-and-api/commands/baseline
    void baselineTest() throws SQLException {
        try (Statement statement = DriverManager.getConnection(jdbcUrl()).createStatement()) {
            statement.execute(
                    "CREATE TABLE series " +
                            "(" +
                            "    series_id Uint64," +
                            "    title Utf8," +
                            "    series_info Utf8," +
                            "    release_date Uint64," +
                            "    PRIMARY KEY (series_id) " +
                            ");" +
                            "CREATE TABLE seasons" +
                            "(" +
                            "    series_id Uint64," +
                            "    season_id Uint64," +
                            "    title Utf8," +
                            "    first_aired Uint64," +
                            "    last_aired Uint64," +
                            "    PRIMARY KEY (series_id, season_id)" +
                            ");" +
                            "CREATE TABLE episodes" +
                            "(" +
                            "    series_id Uint64," +
                            "    season_id Uint64," +
                            "    episode_id Uint64," +
                            "    title Utf8," +
                            "    air_date Uint64," +
                            "    PRIMARY KEY (series_id, season_id, episode_id)" +
                            ");"
            );
        }

        assertTrue(createFlyway("empty").baseline().successfullyBaselined);
    }

    @Test
    void schemaHistoryClean() {

    }

    void verifyTest() {
        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            assertCountTable(2, "SELECT COUNT(*) FROM series", connection);
            assertCountTable(9, "SELECT COUNT(*) FROM seasons", connection);
            assertCountTable(70, "SELECT COUNT(*) FROM episodes", connection);

            ResultSet rs = connection.createStatement().executeQuery("SELECT script FROM flyway_schema_history;");
            HashSet<String> scripts = new HashSet<>();

            while (rs.next()) {
                scripts.add(rs.getString(1));
            }

            assertEquals(EXPECTED_SCRIPTS, scripts);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void cleanYdb() throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            connection.createStatement().execute("DROP TABLE series; DROP TABLE seasons; " +
                    "DROP TABLE episodes; DROP TABLE flyway_schema_history;");
        }
    }

    private static void assertCountTable(int expectedSize, String sql, Connection connection) throws SQLException {
        Statement statement = connection.createStatement();

        ResultSet rs = statement.executeQuery(sql);
        rs.next();

        assertEquals(expectedSize, rs.getLong(1));
    }

    private static Flyway createFlyway(String migrationsDir) {
        return Flyway.configure()
                .locations(migrationsDir)
                .dataSource(jdbcUrl(), "", "")
                .load();
    }

    private static String jdbcUrl() {
        StringBuilder jdbc = new StringBuilder("jdbc:ydb:")
                .append(ydb.useTls() ? "grpcs://" : "grpc://")
                .append(ydb.endpoint())
                .append(ydb.database());

        if (ydb.authToken() != null) {
            jdbc.append("?").append("token=").append(ydb.authToken());
        }

        return jdbc.toString();
    }
}
