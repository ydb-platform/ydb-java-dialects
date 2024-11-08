package tech.ydb.hibernate.ydb_jdbc_code;

import java.math.BigDecimal;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.exception.GenericJDBCException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
public class YdbJdbcCodeTests {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @Test
    public void integrationTests() {
        SESSION_FACTORY = basedConfiguration()
                .addAnnotatedClass(TestEntity.class)
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb) + "?disablePrepareDataQuery=true")
                .buildSessionFactory();

        /*
        create table hibernate_test (
            default_bigDecimal Decimal(22, 9),
            bigDecimal31_9 Decimal(31, 9),
            bigDecimal35_0 Decimal(35, 0),
            bigDecimal35_9 Decimal(35, 9),
            id Uint8 not null,
            primary key (id)
        )
         */
        var testEntity = new TestEntity(
                1,
                new BigDecimal("123.000000000"),
                new BigDecimal("123.000000000"),
                new BigDecimal("12345678123"),
                new BigDecimal("12345678123.000000000")
        );

        inTransaction(session -> session.persist(testEntity));
        inTransaction(session -> assertEquals(testEntity, session.find(TestEntity.class, 1)));

        testEntity.setBigDecimal35_0(new BigDecimal(123));

        inTransaction(session -> session.merge(testEntity));
        inTransaction(session -> assertEquals(testEntity, session.find(TestEntity.class, 1)));

        testEntity.setBigDecimal31_9(new BigDecimal("123123456781231234567812312345678123123456781231234567812312345678123123456781231234567812312345678123"));

        assertThrows(GenericJDBCException.class, () -> inTransaction(session -> session.merge(testEntity)), "Unable to bind parameter #1 - 123123456781231234567812312345678123123456781231234567812312345678123123456781231234567812312345678123 [Cannot cast to decimal type Decimal(31, 9): [class java.math.BigDecimal: 123123456781231234567812312345678123123456781231234567812312345678123123456781231234567812312345678123] is Infinite] [n/a]");

        testEntity.setBigDecimal31_9(new BigDecimal("123451234512345123451234512345123"));

        assertThrows(GenericJDBCException.class, () -> inTransaction(session -> session.merge(testEntity)), "Unable to bind parameter #1 - 123451234512345123451234512345123 [Cannot cast to decimal type Decimal(31, 9): [class java.math.BigDecimal: 123123456781231234567812312345678123123456781231234567812312345678123123456781231234567812312345678123] is Infinite] [n/a]");
    }
}
