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
                assertCountTable(2, "SELECT COUNT(*) FROM series", statement);
                assertCountTable(9, "SELECT COUNT(*) FROM seasons", statement);
                assertCountTable(70, "SELECT COUNT(*) FROM episodes", statement);

                ResultSet rs = statement.executeQuery("SELECT script FROM flyway_schema_history;");
                HashSet<String> scripts = new HashSet<>();

                while (rs.next()) {
                    scripts.add(rs.getString(1));
                }

                assertEquals(expectedScripts(), scripts);
            }
        }
    }

    protected Set<String> expectedScripts() {
        return Set.of();
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

