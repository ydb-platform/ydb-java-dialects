package tech.ydb.hibernate.string;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.ydb.hibernate.TestUtils.*;

/**
 * @author Ainur Mukhtarov
 */
public class StringFunctionsTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @BeforeAll
    static void beforeAll() {
        SESSION_FACTORY = basedConfiguration()
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb))
                .buildSessionFactory();
    }

    @Test
    void lowerFunctionTest() {
        inTransaction(session -> assertEquals("lower text 123", session
                .createQuery("select lower('LoWer Text 123')")
                .getSingleResult()));
    }

    @Test
    void upperFunctionTest() {
        inTransaction(session -> assertEquals("LOWER TEXT 123", session
                .createQuery("select upper('LoWer Text 123')")
                .getSingleResult()));
    }

    @Test
    void concatFunctionTest() {
        inTransaction(session -> assertEquals("123", session
                .createQuery("select concat('1', '2', '3')")
                .getSingleResult()));

        inTransaction(session -> assertEquals("text", session
                .createQuery("select concat('text')")
                .getSingleResult()));
    }
}
