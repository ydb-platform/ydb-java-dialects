package tech.ydb.hibernate.user;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tech.ydb.hibernate.BaseTest;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Kirill Kurdyukov
 */
public class UserRepositoryTest extends BaseTest {

    @BeforeAll
    static void beforeAll() {
        SESSION_FACTORY = basedConfiguration()
                .addAnnotatedClass(User.class)
                .setProperty(AvailableSettings.URL, jdbcUrl())
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
    }

    @Test
    void currentTimestampTest() {
        assertDoesNotThrow(() -> inTransaction(
                session -> {
                    Timestamp now = session.createQuery("select current timestamp", Timestamp.class)
                            .getSingleResult();

                    Date date = session
                            .createQuery("select current date", Date.class).getSingleResult();

                    Time localDateTime = session
                            .createQuery("select current time", Time.class).getSingleResult();
                }
        ));
    }
}
