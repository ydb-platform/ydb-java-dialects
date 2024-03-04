package tech.ydb.liquibase;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * @author Kirill Kurdyukov
 */
public class YdbDatabaseCSVLoadTest extends BaseTest {

    @Test
    void changeLogLoadCSVFileTest() throws SQLException, LiquibaseException {
        String changeLogFile = "./changelogs/changelog-load-csv.xml";

        String migrationStr = migrationStr(changeLogFile);

        assertTrue(migrationStr.contains(
                "UPSERT INTO all_types_table (id, bool_column, bigint_column, float_column, " +
                        "double_column, decimal_column, text_column, binary_column, json_column, " +
                        "jsondocument_column, date_column, datetime_column, timestamp_column, " +
                        "interval_column) VALUES ('1', 'true', '123123', '1.123', '1.123123', " +
                        "'1.123123', 'Кирилл Курдюков Алексеевич', 'binary', '{\"asd\": \"asd\"}'," +
                        " '{\"asd\": \"asd\"}', '2014-04-06', '2023-09-16T12:30', '2023-07-31T17:00:00.000000Z', '123');\n"
        ));

        assertTrue(migrationStr.contains(
                "UPSERT INTO all_types_table" +
                        " (id, bool_column, bigint_column, float_column," +
                        " double_column, decimal_column, text_column, binary_column," +
                        " json_column, jsondocument_column, date_column, datetime_column, " +
                        "timestamp_column, interval_column) VALUES " +
                        "('5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, " +
                        "NULL, NULL, NULL, NULL, NULL);\n"
        ));

        assertTrue(migrationStr.contains(
                "INSERT INTO all_types_table " +
                        "(id, bool_column, bigint_column, float_column, " +
                        "double_column, decimal_column, text_column, " +
                        "binary_column, json_column, jsondocument_column, " +
                        "date_column, datetime_column, timestamp_column, interval_column) " +
                        "VALUES ('2', 'true', '123123', '1.123', '1.123123', '1.123123', " +
                        "'Кирилл Курдюков Алексеевич', 'binary', '{\"asd\": \"asd\"}', " +
                        "'{\"asd\": \"asd\"}', '2014-04-06', '2023-09-16T12:30'," +
                        " '2023-07-31T17:00:00.000000Z', '123'), ('3', 'true', '123123', " +
                        "'1.123', '1.123123', '1.123123', 'Кирилл Курдюков Алексеевич', " +
                        "'binary', '{\"asd\": \"asd\"}', '{\"asd\": \"asd\"}', '2014-04-06', " +
                        "'2023-09-16T12:30', '2023-07-31T17:00:00.000000Z', '123'), " +
                        "('6', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);\n"
        ));

        migrateChangeFile(changeLogFile);
    }

    @Test
    void changeLoadDateBatchInsertTest() throws SQLException, LiquibaseException {
        String changeLogFile = "./changelogs/changelog_batch_load_data.xml";

        String migrationStr = migrationStr(changeLogFile);

        assertTrue(migrationStr.contains(
                "INSERT INTO episodes (series_id, season_id, episode_id, title, air_date) VALUES " +
                        "('1', '1', '1', 'Yesterday\\'sJam', '2006-02-03'), " +
                        "('1', '1', '2', 'CalamityJen', '2006-02-03'), " +
                        "('1', '1', '3', 'Fifty-Fifty', '2006-02-10'), " +
                        "('1', '1', '4', 'TheRedDoor', '2006-02-17'), " +
                        "('1', '1', '5', 'TheHauntingofBillCrouse', '2006-02-24'), " +
                        "('1', '1', '6', 'AuntIrmaVisits', '2006-03-03'), " +
                        "('1', '2', '1', 'TheWorkOuting', '2006-08-24'), " +
                        "('1', '2', '2', 'ReturnoftheGoldenChild', '2007-08-31'), " +
                        "('1', '2', '3', 'MossandtheGerman', '2007-09-07'), " +
                        "('1', '2', '4', 'TheDinnerParty', '2007-09-14'), " +
                        "('1', '2', '5', 'SmokeandMirrors', '2007-09-21'), " +
                        "('1', '2', '6', 'MenWithoutWomen', '2007-09-28'), " +
                        "('1', '3', '1', 'FromHell', '2008-11-21'), " +
                        "('1', '3', '2', 'AreWeNotMen?', '2008-11-28'), " +
                        "('1', '3', '3', 'TrampsLikeUs', '2008-12-05'), " +
                        "('1', '3', '4', 'TheSpeech', '2008-12-12'), " +
                        "('1', '3', '5', 'Friendface', '2008-12-19'), " +
                        "('1', '3', '6', 'CalendarGeeks', '2008-12-26'), " +
                        "('1', '4', '1', 'JenTheFredo', '2010-06-25'), " +
                        "('1', '4', '2', 'TheFinalCountdown', '2010-07-02');\n"
        ));

        assertTrue(migrationStr.contains(
                "INSERT INTO episodes (series_id, season_id, episode_id, title, air_date) VALUES " +
                        "('1', '4', '3', 'SomethingHappened', '2010-07-09'), " +
                        "('1', '4', '4', 'ItalianForBeginners', '2010-07-16'), " +
                        "('1', '4', '5', 'BadBoys', '2010-07-23'), " +
                        "('1', '4', '6', 'ReynholmvsReynholm', '2010-07-30'), " +
                        "('2', '1', '1', 'MinimumViableProduct', '2014-04-06'), " +
                        "('2', '1', '2', 'TheCapTable', '2014-04-13'), " +
                        "('2', '1', '3', 'ArticlesofIncorporation', '2014-04-20'), " +
                        "('2', '1', '4', 'FiduciaryDuties', '2014-04-27'), " +
                        "('2', '1', '5', 'SignalingRisk', '2014-05-04'), " +
                        "('2', '1', '6', 'ThirdPartyInsourcing', '2014-05-11'), " +
                        "('2', '1', '7', 'ProofofConcept', '2014-05-18'), " +
                        "('2', '1', '8', 'OptimalTip-to-TipEfficiency', '2014-06-01'), " +
                        "('2', '2', '1', 'SandHillShuffle', '2015-04-12'), " +
                        "('2', '2', '2', 'RunawayDevaluation', '2015-04-19'), " +
                        "('2', '2', '3', 'BadMoney', '2015-04-26'), " +
                        "('2', '2', '4', 'TheLady', '2015-05-03'), " +
                        "('2', '2', '5', 'ServerSpace', '2015-05-10'), " +
                        "('2', '2', '6', 'Homicide', '2015-05-17'), " +
                        "('2', '2', '7', 'AdultContent', '2015-05-24'), " +
                        "('2', '2', '8', 'WhiteHat/BlackHat', '2015-05-31');\n"
        ));

        assertTrue(migrationStr.contains(
                "INSERT INTO episodes (series_id, season_id, episode_id, title, air_date) VALUES " +
                        "('2', '2', '9', 'BindingArbitration', '2015-06-07'), " +
                        "('2', '2', '10', 'TwoDaysoftheCondor', '2015-06-14'), " +
                        "('2', '3', '1', 'FounderFriendly', '2016-04-24'), " +
                        "('2', '3', '2', 'TwointheBox', '2016-05-01'), " +
                        "('2', '3', '3', 'Meinertzhagen\\'sHaversack', '2016-05-08'), " +
                        "('2', '3', '4', 'MaleantDataSystemsSolutions', '2016-05-15'), " +
                        "('2', '3', '5', 'TheEmptyChair', '2016-05-22'), " +
                        "('2', '3', '6', 'BachmanityInsanity', '2016-05-29'), " +
                        "('2', '3', '7', 'ToBuildaBetterBeta', '2016-06-05'), " +
                        "('2', '3', '8', 'Bachman\\'sEarningsOver-Ride', '2016-06-12'), " +
                        "('2', '3', '9', 'DailyActiveUsers', '2016-06-19'), " +
                        "('2', '3', '10', 'TheUptick', '2016-06-26'), " +
                        "('2', '4', '1', 'SuccessFailure', '2017-04-23'), " +
                        "('2', '4', '2', 'TermsofService', '2017-04-30'), " +
                        "('2', '4', '3', 'IntellectualProperty', '2017-05-07'), " +
                        "('2', '4', '4', 'TeambuildingExercise', '2017-05-14'), " +
                        "('2', '4', '5', 'TheBloodBoy', '2017-05-21'), " +
                        "('2', '4', '6', 'CustomerService', '2017-05-28'), " +
                        "('2', '4', '7', 'ThePatentTroll', '2017-06-04'), " +
                        "('2', '4', '8', 'TheKeenanVortex', '2017-06-11');\n"
        ));

        assertTrue(migrationStr.contains(
                "INSERT INTO episodes (series_id, season_id, episode_id, title, air_date) VALUES " +
                        "('2', '4', '9', 'Hooli-Con', '2017-06-18'), " +
                        "('2', '4', '10', 'ServerError', '2017-06-25'), " +
                        "('2', '5', '1', 'GrowFastorDieSlow', '2018-03-25'), " +
                        "('2', '5', '2', 'Reorientation', '2018-04-01'), " +
                        "('2', '5', '3', 'ChiefOperatingOfficer', '2018-04-08'), " +
                        "('2', '5', '4', 'TechEvangelist', '2018-04-15'), " +
                        "('2', '5', '5', 'FacialRecognition', '2018-04-22'), " +
                        "('2', '5', '6', 'ArtificialEmotionalIntelligence', '2018-04-29'), " +
                        "('2', '5', '7', 'InitialCoinOffering', '2018-05-06'), " +
                        "('2', '5', '8', 'Fifty-OnePercent', '2018-05-13');\n"
        ));

        try (PreparedStatement select = DriverManager.getConnection(jdbcUrl())
                .prepareStatement("select count() as cnt from episodes")) {
            ResultSet rs = select.executeQuery();
            rs.next();
            Assertions.assertEquals(70, rs.getLong("cnt"));
        }

        migrateChangeFile(changeLogFile);
    }
}
