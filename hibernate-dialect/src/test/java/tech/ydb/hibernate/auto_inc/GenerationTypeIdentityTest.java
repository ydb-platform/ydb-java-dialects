package tech.ydb.hibernate.auto_inc;

import org.hibernate.MappingException;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import static tech.ydb.hibernate.TestUtils.SESSION_FACTORY;
import static tech.ydb.hibernate.TestUtils.basedConfiguration;
import static tech.ydb.hibernate.TestUtils.jdbcUrl;
import tech.ydb.test.junit5.YdbHelperExtension;

/**
 * @author Kirill Kurdyukov
 */
public class GenerationTypeIdentityTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

//    @Test
    void serialTypesTest() {
        /*
        create table test_table_int_auto_inc (
            id Serial,
            text Text,
            primary key (id)
        )
         */

        SESSION_FACTORY = basedConfiguration()
                .addAnnotatedClass(TestEntityInt.class)
                .addAnnotatedClass(TestEntityLong.class)
                .addAnnotatedClass(TestEntityShort.class)
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb))
                .buildSessionFactory();

        Assertions.assertThrows(MappingException.class, () -> basedConfiguration()
                .addAnnotatedClass(TestEntityFail.class)
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb))
                .buildSessionFactory());

        SESSION_FACTORY.inTransaction(
                session -> {
                    var intE = new TestEntityInt();
                    intE.setText("test");
                    session.persist(intE);
                    var intS = new TestEntityShort();
                    intS.setText("test");
                    session.persist(intS);
                    var intL = new TestEntityLong();
                    intL.setText("test");
                    session.persist(intL);
                }
        );
    }
}
