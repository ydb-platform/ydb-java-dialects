package tech.ydb.liquibase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import liquibase.exception.CommandExecutionException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabaseUpdateChangeLogTest extends BaseTest {

    @Test
    void integrationTest() throws CommandExecutionException, SQLException {
        migrateChangeFile("./changelogs/changelog-init.xml");
        try (PreparedStatement ps = DriverManager.getConnection(jdbcUrl()).prepareStatement(
                "INSERT INTO DATABASECHANGELOG(" +
                        "ID, AUTHOR, FILENAME, DATEEXECUTED, " +
                        "ORDEREXECUTED, EXECTYPE, MD5SUM, " +
                        "DESCRIPTION, COMMENTS, TAG, LIQUIBASE, " +
                        "CONTEXTS, LABELS, DEPLOYMENT_ID) " +
                        "VALUES (?, 'kurdyukov-kir', 'stub-file.xml', DATETIME('2024-04-01T11:30:20Z'), ?, " +
                        "'EXECUTED', '9:cb49879b530528bc2555422bb7db58da', 'Stub', " +
                        "'', '', '4.25.1', '', '', '1971019939')"
        )) {
            for (int i = 0; i < 210; i++) {
                ps.setString(1, String.valueOf(i));
                ps.setInt(2, i);

                ps.executeUpdate();
            }
        }

        migrateChangeFile("./changelogs/update/changelog-step-1.xml");
        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            ResultSet resultSet = connection.createStatement()
                    .executeQuery("SELECT COUNT(*) AS cnt FROM test WHERE token is NULL");
            assertTrue(resultSet.next());
            assertEquals(3, resultSet.getLong("cnt"));
        }

        migrateChangeFile("./changelogs/update/changelog-step-2.xml");
        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            ResultSet resultSet = connection.createStatement()
                    .executeQuery("SELECT COUNT(*) AS cnt FROM test WHERE token is NULL");
            assertTrue(resultSet.next());
            assertEquals(0, resultSet.getLong("cnt"));
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            connection.createStatement().execute("DROP TABLE test");
        }
    }
}
