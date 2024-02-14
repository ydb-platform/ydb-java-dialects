package tech.ydb.liquibase;

import java.sql.SQLException;
import liquibase.exception.LiquibaseException;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabaseLiquibaseChangeLogStateTest extends BaseTest {

    private static final String DATABASECHANGELOGLOCK = "-- Create Database Lock Table\n" +
            "CREATE TABLE DATABASECHANGELOGLOCK (" +
            "ID INT32, " +
            "LOCKED BOOL, " +
            "LOCKGRANTED DATETIME, " +
            "LOCKEDBY TEXT, " +
            "PRIMARY KEY(ID)" +
            ");";

    private static final String DATABASECHANGELOG = "-- Create Database Change Log Table\n" +
            "CREATE TABLE DATABASECHANGELOG (ID TEXT, " +
            "AUTHOR TEXT, FILENAME TEXT, DATEEXECUTED DATETIME, " +
            "ORDEREXECUTED INT32, EXECTYPE TEXT, MD5SUM TEXT, " +
            "DESCRIPTION TEXT, COMMENTS TEXT, TAG TEXT, " +
            "LIQUIBASE TEXT, CONTEXTS TEXT, LABELS TEXT, " +
            "DEPLOYMENT_ID TEXT, PRIMARY KEY(ID, AUTHOR, FILENAME)" +
            ");";

    @Test
    void liquibaseIntegrationDataChangeLogStateTest() throws SQLException, LiquibaseException {
        changeLogStep1();

        changeLogStep2();

        changeLogStep3();

        changeLogStep3DoNothing();
    }

    private static void changeLogStep1() throws SQLException, LiquibaseException {
        String changeLogFile = "./changelogs/changelog-step-1.xml";

        String outputMigration = migrationStr(changeLogFile);

        // creating meta tables
        assertTrue(outputMigration.contains(DATABASECHANGELOGLOCK));
        assertTrue(outputMigration.contains(DATABASECHANGELOG));

        // migration changelog
        assertTrue(
                outputMigration.contains(
                        "-- Changeset changelogs/migration/series.xml::series::kurdyukov-kir\n" +
                                "-- Table series.\n" +
                                "CREATE TABLE series (series_id INT64, title TEXT, series_info TEXT, release_date date, PRIMARY KEY (series_id) );\n" +
                                "\n" +
                                "ALTER TABLE series ADD INDEX series_index GLOBAL ON (title);\n" +
                                "\n" +
                                "INSERT INTO DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('series', 'kurdyukov-kir', 'changelogs/migration/series.xml', CurrentUtcDatetime(), 1, '9:5809802102bcd74f1d8bc0f1d874463f', 'createTable tableName=series; createIndex indexName=series_index, tableName=series', 'Table series.', 'EXECUTED', NULL, NULL, '4.24.0', NULL);\n" +
                                "\n" +
                                "-- Changeset changelogs/migration/series.xml::added_data_into_series::kurdyukov-kir\n" +
                                "INSERT INTO series (series_id, title, series_info, release_date) VALUES (1, 'IT Crowd', 'The IT Crowd is a British sitcom produced by Channel 4, written by Graham Linehan, produced by Ash Atalla and starring Chris O\\'Dowd, Richard Ayoade, Katherine Parkinson, and Matt Berry.', DATE('2006-02-03'));\n" +
                                "\n" +
                                "INSERT INTO series (series_id, title, series_info, release_date) VALUES (2, 'Silicon Valley', 'Silicon Valley is an American comedy television series created by Mike Judge, John Altschuler and Dave Krinsky. The series focuses on five young men who founded a startup company in Silicon Valley.', DATE('2014-04-06'));\n"
                )
        );

        migrateChangeFile(changeLogFile);
    }

    private static void changeLogStep2() throws SQLException, LiquibaseException {
        String changeLogFile = "./changelogs/changelog-step-2.xml";

        String outputMigration = migrationStr(changeLogFile);

        // not create meta tables
        assertFalse(outputMigration.contains(DATABASECHANGELOGLOCK));
        assertFalse(outputMigration.contains(DATABASECHANGELOG));

        assertTrue(
                outputMigration.contains(
                        "-- Changeset changelogs/migration/seasons_and_episodes.xml::seasons::kurdyukov-kir\n" +
                                "-- Table seasons.\n" +
                                "CREATE TABLE seasons (series_id INT64, season_id INT64, title TEXT, first_aired DATETIME, last_aired DATETIME, PRIMARY KEY (series_id, season_id) );\n" +
                                "\n" +
                                "INSERT INTO seasons (series_id, season_id, title, first_aired, last_aired) VALUES (1, 1, 'Season 1', DATETIME('2019-09-16T07:00:00Z'), DATETIME('2023-09-16T09:30:00Z'));\n" +
                                "\n" +
                                "INSERT INTO DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('seasons', 'kurdyukov-kir', 'changelogs/migration/seasons_and_episodes.xml', CurrentUtcDatetime(), 3, '9:431b823cc76cb5d1f7703812c25bd64f', 'createTable tableName=seasons; insert tableName=seasons', 'Table seasons.', 'EXECUTED', NULL, NULL, '4.24.0', NULL);\n" +
                                "\n" +
                                "-- Changeset changelogs/migration/seasons_and_episodes.xml::episodes::kurdyukov-kir\n" +
                                "-- Table episodes.\n" +
                                "CREATE TABLE episodes (series_id INT64, season_id INT64, episode_id INT64, title TEXT, air_date TIMESTAMP, PRIMARY KEY (series_id, season_id, episode_id) );\n" +
                                "\n" +
                                "INSERT INTO episodes (series_id, season_id, episode_id, title, air_date) VALUES (1, 1, 1, 'Yesterday\\'s Jam', DATETIME('2023-04-01T09:00:00Z'));\n" +
                                "\n" +
                                "INSERT INTO DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID) VALUES ('episodes', 'kurdyukov-kir', 'changelogs/migration/seasons_and_episodes.xml', CurrentUtcDatetime(), 4, '9:35e4ca16ad165b61777356a485eff432', 'createTable tableName=episodes; insert tableName=episodes', 'Table episodes.', 'EXECUTED', NULL, NULL, '4.24.0', NULL);\n"
                )
        );

        migrateChangeFile(changeLogFile);
    }

    private static void changeLogStep3() throws SQLException, LiquibaseException {
        String changeLogFile = "./changelogs/changelog-step-3.xml";

        String outputMigration = migrationStr(changeLogFile);

        // not create meta tables
        assertFalse(outputMigration.contains(DATABASECHANGELOGLOCK));
        assertFalse(outputMigration.contains(DATABASECHANGELOG));

        assertTrue(
                outputMigration.contains(
                        "-- Changeset changelogs/migration/alter_table.xml::alter_table::kurdyukov-kir\n" +
                                "-- Alter table episodes.\n" +
                                "ALTER TABLE seasons ADD is_deleted BOOL;\n" +
                                "\n" +
                                "ALTER TABLE seasons DROP COLUMN first_aired;\n" +
                                "\n" +
                                "ALTER TABLE series DROP INDEX series_index;\n" +
                                "\n" +
                                "DROP TABLE episodes;\n"
                )
        );

        migrateChangeFile(changeLogFile);
    }

    private static void changeLogStep3DoNothing() throws SQLException, LiquibaseException {
        String changeLogFile = "./changelogs/changelog-step-3.xml";

        String outputMigration = migrationStr(changeLogFile);

        assertFalse(outputMigration.contains("ALTER TABLE"));
        assertFalse(outputMigration.contains("CREATE TABLE"));
        assertFalse(outputMigration.contains("DROP TABLE"));
    }
}
