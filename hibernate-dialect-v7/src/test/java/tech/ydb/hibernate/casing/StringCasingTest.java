package tech.ydb.hibernate.casing;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.hibernate.TestUtils;
import tech.ydb.test.junit5.YdbHelperExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.ydb.hibernate.TestUtils.*;

/**
 * @author Ainur Mukhtarov
 */
public class StringCasingTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @BeforeAll
    static void beforeAll() {
        TestUtils.SESSION_FACTORY = basedConfiguration()
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
        inTransaction(session -> assertEquals("UPPER TEXT 123", session
                .createQuery("select upper('UpPer Text 123')")
                .getSingleResult()));
    }
}
