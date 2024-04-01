package tech.ydb.liquibase;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.Test;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabaseReadBigChangeLogTest extends BaseTest {

    private static final int LIMIT_ROWS = 1001;

    @Test
    void readBigChangeLogTest() throws SQLException, LiquibaseException {
        migrateChangeFile("./changelogs/changelog-init.xml");

        // Table Service has limit 1000 rows
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
            for (int i = 0; i < LIMIT_ROWS; i++) {
                ps.setString(1, String.valueOf(i));
                ps.setInt(2, i);

                ps.executeUpdate();
            }
        }

        migrateChangeFile("./changelogs/changelog-step-1.xml");
        migrateChangeFile("./changelogs/changelog-step-2.xml");
        migrateChangeFile("./changelogs/changelog-step-3.xml");
    }
}
