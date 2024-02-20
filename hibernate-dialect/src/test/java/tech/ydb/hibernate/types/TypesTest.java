package tech.ydb.hibernate.types;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TimeZone;
import org.hibernate.cfg.AvailableSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.hibernate.TestUtils;
import static tech.ydb.hibernate.TestUtils.basedConfiguration;
import static tech.ydb.hibernate.TestUtils.inTransaction;
import static tech.ydb.hibernate.TestUtils.jdbcUrl;
import tech.ydb.test.junit5.YdbHelperExtension;

/**
 * @author Kirill Kurdyukov
 */
public class TypesTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @BeforeAll
    static void beforeAll() {
        TestUtils.SESSION_FACTORY = basedConfiguration()
                .addAnnotatedClass(Employee.class)
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb))
                .buildSessionFactory();
    }

    @Test
    void integrationTypesTest() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        Employee employee = new Employee(
                1,
                "Kirill Kurdyukov",
                "kurdyukov-kir@ydb.tech",
                LocalDate.parse("2023-12-20"),
                new BigDecimal("500000.000000000"),
                true,
                "YDB AppTeam",
                23,
                LocalDateTime.parse("2023-09-16T12:30:00"),
                new byte[]{1, 2, 3, 4, 5}
        );

        inTransaction(session -> session.persist(employee));
        inTransaction(session -> assertEquals(employee, session.find(Employee.class, employee.getId())));
    }
}
