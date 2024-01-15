package tech.ydb.hibernate.user;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.hibernate.TestUtils;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.ydb.hibernate.TestUtils.basedConfiguration;
import static tech.ydb.hibernate.TestUtils.inTransaction;
import static tech.ydb.hibernate.TestUtils.jdbcUrl;

/**
 * @author Kirill Kurdyukov
 */
public class UserRepositoryTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @BeforeAll
    static void beforeAll() {
        TestUtils.SESSION_FACTORY = basedConfiguration()
                .addAnnotatedClass(User.class)
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb))
                .buildSessionFactory();
    }

    @Test
    void integrationTest() {
        User user = new User();

        user.setId(1);
        user.setName("Kirill");

        inTransaction(session -> session.persist(user));

        inTransaction(
                session -> {
                    User findUser = session.find(User.class, 1);

                    assertEquals("Kirill", findUser.getName());
                    assertTrue(Instant.now().compareTo(findUser.getCreatedAt()) >= 0);
                    assertTrue(Instant.now().compareTo(findUser.getUpdatedAt()) >= 0);
                }
        );
    }
}
