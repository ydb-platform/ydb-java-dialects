package tech.ydb.hibernate.user;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import org.hibernate.cfg.AvailableSettings;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        var json = new User.Json();

        json.setA(1);
        json.setB("abacaba");

        user.setName("Kirill");
        user.setJson(json);

        inTransaction(session -> session.persist(user));

        inTransaction(
                session -> {
                    User findUser = session.find(User.class, user.getId());

                    assertEquals("Kirill", findUser.getName());
                    assertEquals(json, findUser.getJson());

                    assertTrue(Instant.now().compareTo(findUser.getCreatedAt()) >= 0);
                    assertTrue(Instant.now().compareTo(findUser.getUpdatedAt()) >= 0);
                }
        );

        User rollbackUser = new User();

        user.setId(10);
        user.setName("Kirill");

        try {
            inTransaction(session -> {
                session.persist(rollbackUser);

                throw new RuntimeException();
            });
        } catch (RuntimeException ignored) {
        }

        inTransaction(session -> assertNull(session.find(User.class, 10)));
    }

    @Test
    void currentTimestampTest() {
        assertDoesNotThrow(() -> inTransaction(
                session -> {
                    Timestamp now = session.createQuery("select current timestamp", Timestamp.class)
                            .getSingleResult();

                    Date date = session
                            .createQuery("select current date", Date.class).getSingleResult();

                    LocalDateTime localDateTime = session
                            .createQuery("select current time", LocalDateTime.class)
                            .getSingleResult();
                }
        ));
    }
}
