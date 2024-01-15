package tech.ydb.hibernate.user;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tech.ydb.hibernate.BaseTest;

import java.time.Instant;

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
