package tech.ydb.liquibase;

import java.sql.SQLException;
import liquibase.exception.LiquibaseException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabaseLiquibaseAllTypesTableTest extends BaseTest {

    @Test
    void changelogXmlMigrationAllTypesTableTest() throws SQLException, LiquibaseException {
        String changeLogFile = "./changelogs/changelog_all_types_table.xml";

        String outputMigration = migrationStr(changeLogFile);

        System.out.println(outputMigration);
        assertTrue(
                outputMigration.contains("CREATE TABLE all_types_table (" +
                        "id INT32, bool_column BOOL, bigint_column INT64, " +
                        "float_column FLOAT, double_column DOUBLE, " +
                        "decimal_column DECIMAL(22,9), text_column TEXT, " +
                        "binary_column BYTES, json_column JSON, " +
                        "jsondocument_column JSONDOCUMENT, date_column date, " +
                        "datetime_column DATETIME, timestamp_column TIMESTAMP, " +
                        "interval_column INTERVAL, PRIMARY KEY (id) );"
                )
        );

        migrateChangeFile(changeLogFile);
    }
}
