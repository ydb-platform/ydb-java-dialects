package tech.ydb.flyway.database;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.api.ErrorCode;
import org.flywaydb.core.internal.command.DbMigrate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * @author Kirill Kurdyukov
 */
public class YdbFlywayRepairAndValidateTest extends YdbFlywayBaseTest {

    @Test
    void markUnSuccessMigrationThenRepairDeletedTest() throws SQLException {
        assertThrows(DbMigrate.FlywayMigrateException.class,
                () -> createFlyway("classpath:db/migration-with-failed").load().migrate());

        try (var connection = DriverManager.getConnection(jdbcUrl())) {
            try (var st = connection.createStatement()) {
                var rs = st.executeQuery("SELECT script FROM flyway_schema_history WHERE success = false");

                rs.next();

                assertEquals("V3__create_episodes.sql", rs.getString(1));

                assertFalse(rs.next());
            }
        }

        createFlyway("classpath:db/migration-step-3").load().repair();

        try (var connection = DriverManager.getConnection(jdbcUrl())) {
            try (var st = connection.createStatement()) {
                var rs = st.executeQuery("SELECT COUNT(*) FROM flyway_schema_history");

                rs.next();

                assertEquals(2, rs.getLong(1));
            }
        }

        assertTrue(createFlyway("classpath:db/migration").load().migrate().success);
    }

    @Test
    void updateChecksumMigrationTest() {
        assertTrue(createFlyway("classpath:db/migration-step-3").load().migrate().success);

        String checksumPrev = checksum();

        var flyway = createFlyway("classpath:db/migration-with-failed").load();

        var validateResult = flyway.validateWithResult();

        assertFalse(validateResult.validationSuccessful);
        assertEquals("create episodes", validateResult.invalidMigrations.get(0).description);
        assertEquals(ErrorCode.CHECKSUM_MISMATCH, validateResult.invalidMigrations.get(0).errorDetails.errorCode);

        flyway.repair();

        assertNotEquals(checksumPrev, checksum());

        flyway = createFlyway("classpath:db/migration").load();

        flyway.repair();

        assertEquals(checksumPrev, checksum());

        assertTrue(flyway.migrate().success);
    }

    private static String checksum() {
        try (var connection = DriverManager.getConnection(jdbcUrl())) {
            var rs = connection.createStatement().executeQuery("SELECT checksum FROM flyway_schema_history WHERE version = '3'");

            rs.next();
            return rs.getString(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
