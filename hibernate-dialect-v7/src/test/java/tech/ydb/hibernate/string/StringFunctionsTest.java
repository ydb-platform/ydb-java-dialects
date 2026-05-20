package tech.ydb.hibernate.string;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.util.List;

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
                .addAnnotatedClass(StringEntity.class)
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb))
                .buildSessionFactory();
    }

    @Test
    void lowerFunctionTest() {
        inTransaction(session -> assertEquals("lower text 123", session
                .createQuery("select lower('LoWer Text 123')")
                .getSingleResult()));
        inTransaction(session -> assertEquals("юникод текст", session
                .createQuery("select lower('юНикод ТеКст')")
                .getSingleResult()));
    }

    @Test
    void upperFunctionTest() {
        inTransaction(session -> assertEquals("LOWER TEXT 123", session
                .createQuery("select upper('LoWer Text 123')")
                .getSingleResult()));
        inTransaction(session -> assertEquals("ЮНИКОД ТЕКСТ", session
                .createQuery("select upper('юНикод ТеКст')")
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

    @Test
    void lowerWithConcatTest() {
        inTransaction(session -> assertEquals("lower text 123", session
                .createQuery("select lower(concat('LoWer', ' ', 'Text', ' ', '123'))")
                .getSingleResult()));
    }

    @Test
    void lowerOnColumnWithLikeTest() {
        inTransaction(session -> {
            StringEntity entity = new StringEntity();
            entity.id = 1;
            entity.name = "test entity";
            session.persist(entity);
        });

        inTransaction(session -> {
            List<StringEntity> result = session
                    .createQuery(
                            "select e from StringEntity e where lower(e.name) like lower(concat('%', :search, '%'))",
                            StringEntity.class
                    )
                    .setParameter("search", "test")
                    .getResultList();
            assertEquals(1, result.size());
        });
    }

    @Test
    void likeWithEscapeCharLiteralTest() {
        inTransaction(session -> {
            StringEntity entity = new StringEntity();
            entity.id = 60;
            entity.name = "скидка 50% на всё";
            session.persist(entity);
        });

        inTransaction(session -> {
            List<StringEntity> result = session
                    .createQuery(
                            "select e from StringEntity e where e.name like '%50!%%' escape '!'",
                            StringEntity.class
                    )
                    .getResultList();
            assertEquals(1, result.size());
        });
    }
}