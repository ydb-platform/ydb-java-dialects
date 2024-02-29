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

        String migrationStr = migrationStr(changeLogFile);

        System.out.println(migrationStr);
        assertTrue(migrationStr.contains(
                "UPSERT INTO all_types_table (id, bool_column, bigint_column, float_column, " +
                        "double_column, decimal_column, text_column, binary_column, json_column, " +
                        "jsondocument_column, date_column, datetime_column, timestamp_column, " +
                        "interval_column) VALUES ('1', 'true', '123123', '1.123', '1.123123', " +
                        "'1.123123', 'Кирилл Курдюков Алексеевич', 'binary', '{\"asd\": \"asd\"}'," +
                        " '{\"asd\": \"asd\"}', '2014-04-06', '2023-09-16T12:30', '2023-07-31T17:00:00.000000Z', '123');\n"
        ));

        assertTrue(migrationStr.contains(
                "INSERT INTO all_types_table " +
                        "(id, bool_column, bigint_column, float_column, " +
                        "double_column, decimal_column, text_column, binary_column," +
                        " json_column, jsondocument_column, date_column, datetime_column, " +
                        "timestamp_column, interval_column) " +
                        "VALUES ('2', 'true', '123123', '1.123', '1.123123', " +
                        "'1.123123', 'Кирилл Курдюков Алексеевич', 'binary', " +
                        "'{\"asd\": \"asd\"}', '{\"asd\": \"asd\"}', '2014-04-06', " +
                        "'2023-09-16T12:30', '2023-07-31T17:00:00.000000Z', '123'), " +
                        "('3', 'true', '123123', '1.123', '1.123123', '1.123123', " +
                        "'Кирилл Курдюков Алексеевич', 'binary', '{\"asd\": \"asd\"}', " +
                        "'{\"asd\": \"asd\"}', '2014-04-06', '2023-09-16T12:30', '2023-07-31T17:00:00.000000Z', '123');\n"
        ));

        migrateChangeFile(changeLogFile);
    }
}
