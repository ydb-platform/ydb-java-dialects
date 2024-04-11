package tech.ydb.flyway.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

/**
 * @author Kirill Kurdyukov
 */
abstract class YdbFlywayBaseTest {

    private static final Set<String> EXPECTED_ALL_SCRIPTS = Set.of(
            "V1__create_series.sql", "V2__create_seasons.sql",
            "V3__create_episodes.sql", "V4__load_data.sql",
            "V5__create_series_title_index.sql", "V6__rename_index_title_index.sql"
    );

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    protected static void assertCountTable(int expectedSize, String sql, Statement statement) throws SQLException {
        ResultSet rs = statement.executeQuery(sql);
        rs.next();

        assertEquals(expectedSize, rs.getLong(1));
    }

    protected static FluentConfiguration createFlyway(String migrationsDir) {
        return Flyway.configure()
                .locations(migrationsDir)
                .dataSource(jdbcUrl(), "", "");
    }

    protected static String jdbcUrl() {
        StringBuilder jdbc = new StringBuilder("jdbc:ydb:")
                .append(ydb.useTls() ? "grpcs://" : "grpc://")
                .append(ydb.endpoint())
                .append(ydb.database());

        if (ydb.authToken() != null) {
            jdbc.append("?").append("token=").append(ydb.authToken());
        }

        return jdbc.toString();
    }

    protected void verifyTest() throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            try (Statement statement = connection.createStatement()) {
                verifyCountTables(statement);

                ResultSet rs = statement.executeQuery("SELECT script FROM flyway_schema_history;");
                HashSet<String> scripts = new HashSet<>();

                while (rs.next()) {
                    scripts.add(rs.getString(1));
                }

                assertEquals(expectedScripts(), scripts);
            }
        }
    }

    protected void verifyCountTables(Statement statement) throws SQLException {
        assertCountTable(2, "SELECT COUNT(*) FROM series", statement);
        assertCountTable(9, "SELECT COUNT(*) FROM seasons", statement);
        assertCountTable(70, "SELECT COUNT(*) FROM episodes", statement);
    }

    protected Set<String> expectedScripts() {
        return EXPECTED_ALL_SCRIPTS;
    }

    @AfterEach
    void checkAfterTest() throws SQLException {
        verifyTest();

        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE series; DROP TABLE seasons; " +
                        "DROP TABLE episodes; DROP TABLE flyway_schema_history;");
            }
        }
    }
}

