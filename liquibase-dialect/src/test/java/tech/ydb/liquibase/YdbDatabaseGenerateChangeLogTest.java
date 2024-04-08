package tech.ydb.liquibase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import liquibase.command.CommandScope;
import liquibase.command.core.GenerateChangelogCommandStep;
import liquibase.exception.CommandExecutionException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabaseGenerateChangeLogTest extends BaseTest {

    @Test
    void generateChangeLogFileXmlTest(@TempDir Path tempDir) throws CommandExecutionException, IOException {
        Path changeLogFile = tempDir.resolve("file.xml");

        executeGeneration(changeLogFile);

        String changelog = new String(Files.readAllBytes(changeLogFile));

        assertTrue(changelog.contains(
                "        <createTable tableName=\"all_types_table\">\n" +
                        "            <column name=\"id\" type=\"INT32\">\n" +
                        "                <constraints nullable=\"false\" primaryKey=\"true\"/>\n" +
                        "            </column>\n" +
                        "            <column name=\"bool_column\" type=\"BOOL\"/>\n" +
                        "            <column name=\"bigint_column\" type=\"INT64\"/>\n" +
                        "            <column name=\"smallint_column\" type=\"INT16\"/>\n" +
                        "            <column name=\"tinyint_column\" type=\"INT8\"/>\n" +
                        "            <column name=\"float_column\" type=\"FLOAT\"/>\n" +
                        "            <column name=\"double_column\" type=\"DOUBLE\"/>\n" +
                        "            <column name=\"decimal_column\" type=\"DECIMAL(22,9)\"/>\n" +
                        "            <column name=\"uint8_column\" type=\"UINT8\"/>\n" +
                        "            <column name=\"uint16_column\" type=\"UINT16\"/>\n" +
                        "            <column name=\"unit32_column\" type=\"UINT32\"/>\n" +
                        "            <column name=\"unit64_column\" type=\"UINT64\"/>\n" +
                        "            <column name=\"text_column\" type=\"TEXT\"/>\n" +
                        "            <column name=\"binary_column\" type=\"BYTES\"/>\n" +
                        "            <column name=\"json_column\" type=\"JSON\"/>\n" +
                        "            <column name=\"jsondocument_column\" type=\"JSONDOCUMENT\"/>\n" +
                        "            <column name=\"date_column\" type=\"DATE\"/>\n" +
                        "            <column name=\"datetime_column\" type=\"DATETIME\"/>\n" +
                        "            <column name=\"timestamp_column\" type=\"TIMESTAMP\"/>\n" +
                        "            <column name=\"interval_column\" type=\"INTERVAL\"/>\n" +
                        "        </createTable>"
        ));

        assertTrue(changelog.contains(
                "        <createTable tableName=\"episodes\">\n" +
                        "            <column name=\"series_id\" type=\"INT64\">\n" +
                        "                <constraints primaryKey=\"true\"/>\n" +
                        "            </column>\n" +
                        "            <column name=\"season_id\" type=\"INT64\">\n" +
                        "                <constraints primaryKey=\"true\"/>\n" +
                        "            </column>\n" +
                        "            <column name=\"episode_id\" type=\"INT64\">\n" +
                        "                <constraints primaryKey=\"true\"/>\n" +
                        "            </column>\n" +
                        "            <column name=\"title\" type=\"TEXT\">\n" +
                        "                <constraints nullable=\"false\"/>\n" +
                        "            </column>\n" +
                        "            <column name=\"air_date\" type=\"DATE\"/>\n" +
                        "        </createTable>"
        ));

        assertTrue(changelog.contains(
                "        <createIndex indexName=\"title_index\" tableName=\"episodes\">\n" +
                        "            <column name=\"title\"/>\n" +
                        "        </createIndex>\n" +
                        "    </changeSet>\n"
        ));
    }

    @Test
    void generateChangeLogFileSql(@TempDir Path tempDir) throws CommandExecutionException, IOException {
        Path changeLogFile = tempDir.resolve("changelog.ydb.sql");

        executeGeneration(changeLogFile);

        String changelog = new String(Files.readAllBytes(changeLogFile));

        assertTrue(changelog.contains(
                "CREATE TABLE all_types_table (" +
                        "id INT32 NOT NULL, " +
                        "bool_column BOOL, " +
                        "bigint_column INT64, " +
                        "smallint_column INT16, " +
                        "tinyint_column INT8, " +
                        "float_column FLOAT, " +
                        "double_column DOUBLE, " +
                        "decimal_column DECIMAL(22,9), " +
                        "uint8_column UINT8, " +
                        "uint16_column UINT16, " +
                        "unit32_column UINT32, " +
                        "unit64_column UINT64, " +
                        "text_column TEXT, " +
                        "binary_column BYTES, " +
                        "json_column JSON, " +
                        "jsondocument_column JSONDOCUMENT, " +
                        "date_column DATE, " +
                        "datetime_column DATETIME, " +
                        "timestamp_column TIMESTAMP, " +
                        "interval_column INTERVAL, " +
                        "PRIMARY KEY (id) " +
                        ");"
        ));

        assertTrue(changelog.contains(
                "CREATE TABLE episodes (" +
                        "series_id INT64 NOT NULL, " +
                        "season_id INT64 NOT NULL, " +
                        "episode_id INT64 NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "air_date DATE, " +
                        "PRIMARY KEY (series_id, season_id, episode_id) " +
                        ");"
        ));

        assertTrue(changelog.contains(
                "ALTER TABLE episodes ADD INDEX title_index GLOBAL ON (title);"
        ));
    }

    private static void executeGeneration(Path changeLogFile) throws CommandExecutionException {
        new CommandScope(GenerateChangelogCommandStep.COMMAND_NAME)
                .addArgumentValue("url", jdbcUrl())
                .addArgumentValue(GenerateChangelogCommandStep.CHANGELOG_FILE_ARG, changeLogFile.toString())
                .execute();
    }

    @BeforeEach
    void beforeAll() {
        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            connection.createStatement().execute(
                    "CREATE TABLE all_types_table (" +
                            "id INT32 NOT NULL, " +
                            "bool_column BOOL, " +
                            "bigint_column INT64, " +
                            "smallint_column INT16, " +
                            "tinyint_column INT8, " +
                            "float_column FLOAT, " +
                            "double_column DOUBLE, " +
                            "decimal_column DECIMAL(22,9), " +
                            "uint8_column UINT8, " +
                            "uint16_column UINT16, " +
                            "unit32_column UINT32, " +
                            "unit64_column UINT64, " +
                            "text_column TEXT, " +
                            "binary_column BYTES, " +
                            "json_column JSON, " +
                            "jsondocument_column JSONDOCUMENT, " +
                            "date_column DATE, " +
                            "datetime_column DATETIME, " +
                            "timestamp_column TIMESTAMP, " +
                            "interval_column INTERVAL, " +
                            "PRIMARY KEY (id) " +
                            ");"
            );

            connection.createStatement().execute(
                    "CREATE TABLE episodes\n" +
                            "(" +
                            "    series_id INT64," +
                            "    season_id INT64," +
                            "    episode_id INT64," +
                            "    title TEXT NOT NULL," +
                            "    air_date DATE," +
                            "    PRIMARY KEY (series_id, season_id, episode_id)" +
                            ");" +
                            "ALTER TABLE `episodes` ADD INDEX `title_index` GLOBAL ON (`title`);"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
