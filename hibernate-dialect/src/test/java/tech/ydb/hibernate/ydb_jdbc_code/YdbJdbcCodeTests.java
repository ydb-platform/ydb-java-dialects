package tech.ydb.hibernate.ydb_jdbc_code;

import java.math.BigDecimal;
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
public class YdbJdbcCodeTests {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @Test
    public void integrationTests() {
        SESSION_FACTORY = basedConfiguration()
                .addAnnotatedClass(TestEntity.class)
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb))
                .buildSessionFactory();

        var testEntity = new TestEntity(1, new BigDecimal("123.000000000"), new BigDecimal(123), new BigDecimal("12345678123"));

        inTransaction(session -> session.persist(testEntity));
        inTransaction(session -> assertEquals(testEntity, session.find(TestEntity.class, 1)));
    }
}
