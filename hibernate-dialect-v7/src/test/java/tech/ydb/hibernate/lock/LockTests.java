package tech.ydb.hibernate.lock;

import jakarta.persistence.LockModeType;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.ydb.hibernate.TestUtils.*;
import static tech.ydb.hibernate.dialect.YdbSettings.IGNORE_LOCK_HINTS;

/**
 * Tests for {@value tech.ydb.hibernate.dialect.YdbSettings#IGNORE_LOCK_HINTS} setting.
 *
 * @author Ainur Mukhtarov
 */
public class LockTests {

    private static final long ENTITY_ID = 1L;

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @Test
    void whenIgnoreLockHintsTrue_pessimisticLockSucceeds() {
        runWithConfiguration(true, () -> {
            assertEntityFoundWithLock(LockModeType.PESSIMISTIC_WRITE);
            assertEntityFoundWithLock(LockModeType.PESSIMISTIC_READ);
            assertEntityFoundWithLock(LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        });
    }

    @Test
    void whenIgnoreLockHintsFalse_pessimisticLockThrows() {
        runWithConfiguration(false, () -> {
            assertThrowsWithLock(LockModeType.PESSIMISTIC_WRITE);
            assertThrowsWithLock(LockModeType.PESSIMISTIC_READ);
            assertThrowsWithLock(LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        });
    }

    private void persistEntity() {
        var entity = new TestEntity();
        entity.setId(ENTITY_ID);
        entity.setStringValue("test");

        inTransaction(session -> session.persist(entity));
    }

    private static void assertEntityFoundWithLock(LockModeType lockMode) {
        inTransaction(session -> {
            var actual = session.find(TestEntity.class, ENTITY_ID, lockMode);

            assertEquals(ENTITY_ID, actual.getId());
            assertEquals("test", actual.getStringValue());
        });
    }

    private static void assertThrowsWithLock(LockModeType pessimisticForceIncrement) {
        assertThrows(
            UnsupportedOperationException.class,
            () -> inTransaction(s -> s.find(TestEntity.class, ENTITY_ID, pessimisticForceIncrement)),
            "YDB does not support FOR UPDATE statement"
        );
    }

    private void runWithConfiguration(boolean settingValue, Runnable test) {
        SESSION_FACTORY = basedConfiguration()
            .setProperty(AvailableSettings.URL, jdbcUrl(ydb))
            .addAnnotatedClass(TestEntity.class)
            .setProperty(IGNORE_LOCK_HINTS, settingValue)
            .buildSessionFactory();
        try {
            persistEntity();

            test.run();
        } finally {
            inTransaction(session -> {
                var entity = session.find(TestEntity.class, ENTITY_ID);
                if (entity != null) {
                    session.remove(entity);
                }
            });
            if (SESSION_FACTORY != null) {
                SESSION_FACTORY.close();
            }
        }
    }
}
