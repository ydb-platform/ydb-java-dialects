package tech.ydb.liquibase;

import java.sql.SQLException;
import liquibase.exception.LiquibaseException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabaseCSVLoadTest extends BaseTest {

    @Test
    void changeLogLoadCSVFileWithBoolValueTest() throws SQLException, LiquibaseException {
        String changeLogFile = "./changelogs/changelog-load-csv.xml";

        assertTrue(migrationStr(changeLogFile)
                .contains("INSERT INTO test (code, flag, id) VALUES ('SYS_STATE', 'False', 'Gv9CXQKHkXxVzDYamCXIHQ');"));

        migrateChangeFile(changeLogFile);
    }
}
