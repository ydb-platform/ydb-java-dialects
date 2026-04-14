package tech.ydb.hibernate.exception;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.ydb.hibernate.TestUtils.*;

class ExceptionConversionTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @BeforeAll
    static void setUp() {
        SESSION_FACTORY = basedConfiguration()
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb))
                .setProperty(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "none")
                .addAnnotatedClass(DupTestEntity.class)
                .buildSessionFactory();

        inTransaction(session -> {
            session.createNativeQuery(
                    """
                            CREATE TABLE exception_test_dup (
                                id    Int32  NOT NULL,
                                name  Text,
                                PRIMARY KEY (id),
                                INDEX idx_dup_name GLOBAL UNIQUE ON (name)
                            )"""
            ).executeUpdate();
        });
    }

    @Test
    void duplicatePrimaryKeyThrowsConstraintViolationException() {
        inTransaction(session -> session.persist(new DupTestEntity(1, "pk-first")));

        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> inTransaction(session -> session.persist(new DupTestEntity(1, "pk-duplicate"))));

        assertEquals(ConstraintViolationException.ConstraintKind.UNIQUE, ex.getKind());
    }

    @Test
    void duplicateUniqueIndexThrowsConstraintViolationException() {
        inTransaction(session -> session.persist(new DupTestEntity(10, "unique-value")));

        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> inTransaction(session -> session.persist(new DupTestEntity(11, "unique-value"))));

        assertEquals(ConstraintViolationException.ConstraintKind.UNIQUE, ex.getKind());
    }
}
