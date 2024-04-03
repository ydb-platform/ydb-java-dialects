package tech.ydb.liquibase;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import liquibase.command.CommandScope;
import liquibase.exception.CommandExecutionException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import tech.ydb.jdbc.exception.YdbSQLException;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabaseRollbackChangesetTest extends BaseTest {

    @Test
    void rollbackLiquibaseCommandTest() throws CommandExecutionException, SQLException {
        String changeLogFile = "./changelogs/changelog-step-1.xml";

        migrateChangeFile(changeLogFile);

        try(PreparedStatement ps = DriverManager.getConnection(jdbcUrl())
                .prepareStatement("SELECT COUNT(*) FROM series")) {
            ResultSet rs = ps.executeQuery();

            rs.next();

            assertEquals(0, rs.getInt(1));
        }

        new CommandScope("rollbackCount")
                .addArgumentValue("changeLogFile", changeLogFile)
                .addArgumentValue("url", jdbcUrl())
                .addArgumentValue("count", 1)
                .execute();

        assertThrows(YdbSQLException.class, () -> {
            try(PreparedStatement ps = DriverManager.getConnection(jdbcUrl())
                    .prepareStatement("SELECT COUNT(*) FROM series")) {
                ResultSet rs = ps.executeQuery();

                rs.next();

                assertEquals(0, rs.getInt(1));
            }
        });
    }
}
