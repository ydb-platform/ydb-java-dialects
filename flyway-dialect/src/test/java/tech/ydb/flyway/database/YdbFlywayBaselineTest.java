package tech.ydb.flyway.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/**
 * <a href="https://documentation.red-gate.com/flyway/flyway-cli-and-api/commands/baseline">Baseline command</a>
 *
 * @author Kirill Kurdyukov
 */
public class YdbFlywayBaselineTest extends YdbFlywayBaseTest {

    private static final Set<String> EXPECTED_SCRIPTS = Set.of(
            "<< Flyway Baseline >>", "V5__create_series_title_index.sql",
            "V6__rename_series_title_index.sql", "V4__load_data.sql"
    );

    @Test
    void baselineTest() throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            try (Statement statement = connection.createStatement()) {
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
        }

        Flyway flyway = createFlyway("classpath:db/migration").baselineVersion("3").load();

        flyway.baseline(); // fixed 4 version

        flyway.migrate();
    }

    @Override
    protected Set<String> expectedScripts() {
        return EXPECTED_SCRIPTS;
    }
}
