package tech.ydb.liquibase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import liquibase.command.CommandScope;
import liquibase.exception.CommandExecutionException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabaseTagTest extends BaseTest {

    @Test
    void tagLiquibaseCommandTest() throws CommandExecutionException, SQLException {
        migrateChangeFile("./changelogs/changelog-step-2.xml");

        new CommandScope("tag")
                .addArgumentValue("url", jdbcUrl())
                .addArgumentValue("tag", "test")
                .execute();

        try (Connection connection = DriverManager.getConnection(jdbcUrl());
             ResultSet resultSet = connection.createStatement().executeQuery(
                     "SELECT ID, TAG FROM DATABASECHANGELOG WHERE TAG IS NOT NULL"
             )) {
            assertTrue(resultSet.next());
            assertEquals("episodes", resultSet.getString("ID"));
            assertEquals("test", resultSet.getString("TAG"));
            assertFalse(resultSet.next());
        }
    }
}
