package tech.ydb.liquibase;

import liquibase.exception.CommandExecutionException;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabaseFailSqlStatementMessagesTest extends BaseTest {

    @Test
    void YdbNeedsToHavePRIMARYKEYTest() {
        checkFailMigration(
                "table_not_has_primary_key.xml",
                "Table YDB needs to have a PRIMARY KEY. [table_name = table]"
        );
    }

    @Test
    void YdbDoesNotSupportUniqueConstraintTest() {
        checkFailMigration(
                "table_has_unique_constraint.xml",
                "YDB doesn't support UNIQUE CONSTRAINT! [table_name = table]"
        );
    }

    @Test
    void YdbDoesNotSupportDefaultValueConstraintTest() {
        checkFailMigration(
                "table_has_default_value.xml",
                "YDB doesn't support DEFAULT VALUE CONSTRAINT! [table_name = table]"
        );
    }

    @Test
    void YdbDoesNotSupportAutoIncrementConstraintTest() {
        checkFailMigration(
                "table_has_auto_increment.xml",
                "YDB doesn't support AUTO INCREMENT CONSTRAINT! [table_name = table]"
        );
    }

    @Test
    void YdbDoesNotSupportForeignKeyConstraintTest() {
        checkFailMigration(
                "table_has_foreign_key.xml",
                "YDB doesn't support FOREIGN KEY CONSTRAINT! [table_name = table]"
        );
    }

    private static void checkFailMigration(String fileName, String message) {
        CommandExecutionException exception = Assertions.assertThrows(
                CommandExecutionException.class,
                () -> migrationStr("./changelogs/migration-fail/" + fileName)
        );

        assertEquals(
                "liquibase.exception.ValidationFailedException: Validation Failed:\n" +
                        "     1 changes have validation failures\n" +
                        "          " + message + ", " +
                        "changelogs/migration-fail/" + fileName + "::failed-migration::kurdyukov-kir\n",
                exception.getMessage())
        ;
    }
}
