package tech.ydb.flyway.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * <a href="https://documentation.red-gate.com/fd/clean-184127458.html">Clean command</a>
 *
 * @author Kirill Kurdyukov
 */
public class YdbFlywayCleanTest extends YdbFlywayBaseTest {

    @Test
    void cleanSchemaTest() throws SQLException {
        Flyway flyway = createFlyway("classpath:db/migration").cleanDisabled(false).load();

        assertTrue(flyway.migrate().success);

        verifyTest();

        flyway.clean();

        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            assertDeletedTable(connection, "SELECT * FROM episodes");
            assertDeletedTable(connection, "SELECT * FROM series");
            assertDeletedTable(connection, "SELECT * FROM seasons");
            assertDeletedTable(connection, "SELECT * FROM flyway_schema_history");
        }
    }

    private static void assertDeletedTable(Connection connection, String sql) {
        assertThrows(SQLException.class, () -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        });
    }

    @Override
    void checkAfterTest() {
    }
}
