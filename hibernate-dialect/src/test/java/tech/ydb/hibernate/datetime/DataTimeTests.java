package tech.ydb.hibernate.datetime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.hibernate.cfg.AvailableSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static tech.ydb.hibernate.TestUtils.SESSION_FACTORY;
import static tech.ydb.hibernate.TestUtils.basedConfiguration;
import static tech.ydb.hibernate.TestUtils.inTransaction;
import static tech.ydb.hibernate.TestUtils.jdbcUrl;
import tech.ydb.test.junit5.YdbHelperExtension;

/**
 * @author Kirill Kurdyukov
 */
public class DataTimeTests {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @Test
    void integrationTest() {
        SESSION_FACTORY = basedConfiguration()
                .addAnnotatedClass(TestEntity.class)
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb))
                .buildSessionFactory();

        var date = LocalDate.now();
        var datetime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        var timestamp = Instant.now();

        var expected = new TestEntity();
        expected.setId(1);
        expected.setDate(date);
        expected.setDatetime(datetime);
        expected.setTimestamp(timestamp);

        inTransaction(session -> session.persist(expected));

        inTransaction(session -> {
            var actual = session.find(TestEntity.class, 1);
            
            assertEquals(expected, actual);
        });
    }
}
