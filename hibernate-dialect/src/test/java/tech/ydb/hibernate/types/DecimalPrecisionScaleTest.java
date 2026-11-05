package tech.ydb.hibernate.types;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static tech.ydb.hibernate.TestUtils.basedConfiguration;
import static tech.ydb.hibernate.TestUtils.jdbcUrl;
import tech.ydb.test.junit5.YdbHelperExtension;

public class DecimalPrecisionScaleTest {
    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    private static SessionFactory sessionFactory;

    @BeforeAll
    static void setUp() {
        sessionFactory = basedConfiguration().addAnnotatedClass(DecimalEntity.class)
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb) + "?disablePrepareDataQuery=true")
                .setProperty(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "drop-and-create")
                .setProperty(AvailableSettings.STATEMENT_BATCH_SIZE, "3")
                .buildSessionFactory();
    }

    @AfterAll
    static void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    void persistsBatchWithLargeValuesZerosAndNulls() {
        DecimalEntity large = largeValues(1);
        DecimalEntity zero = new DecimalEntity(2, new BigDecimal("0.00"), new BigDecimal("0.0000000000"),
                new BigDecimal("0.00000000000000000000000000000000000"), BigDecimal.ZERO,
                new BigDecimal("0.000000000"), BigInteger.ZERO);
        DecimalEntity empty = new DecimalEntity(3, null, null, null, null, null, null);
        sessionFactory.inTransaction(session -> {
            session.persist(large);
            session.persist(zero);
            session.persist(empty);
        });
        sessionFactory.inTransaction(session -> {
            assertEquals(large, session.find(DecimalEntity.class, 1L));
            assertEquals(zero, session.find(DecimalEntity.class, 2L));
            assertEquals(empty, session.find(DecimalEntity.class, 3L));
        });
    }

    @Test
    void updatesAndQueriesWithInferredParameters() {
        DecimalEntity entity = largeValues(10);
        sessionFactory.inTransaction(session -> session.persist(entity));
        entity.setLargeAmount(new BigDecimal("-1234567890123456789012345.9876543210"));
        entity.setFraction(new BigDecimal("-0.12345678901234567890123456789012345"));
        sessionFactory.inTransaction(session -> session.createMutationQuery(
                        "update DecimalEntity e set e.largeAmount = :amount, e.fraction = :fraction where e.id = :id")
                .setParameter("amount", entity.getLargeAmount())
                .setParameter("fraction", entity.getFraction())
                .setParameter("id", entity.getId())
                .executeUpdate());
        sessionFactory.inTransaction(session -> assertEquals(entity,
                session.createQuery("from DecimalEntity e where e.largeAmount = :amount and e.fraction = :fraction",
                                DecimalEntity.class)
                        .setParameter("amount", entity.getLargeAmount())
                        .setParameter("fraction", entity.getFraction())
                        .getSingleResult()));
        entity.setIntegerAmount(new BigDecimal("-12345678901234567890123456789012345"));
        sessionFactory.inTransaction(session -> session.merge(entity));
        sessionFactory.inTransaction(session -> assertEquals(entity, session.find(DecimalEntity.class, entity.getId())));
    }

    @Test
    void readsDecimalNativeQueryMetadata() {
        DecimalEntity entity = largeValues(20);
        sessionFactory.inTransaction(session -> session.persist(entity));
        sessionFactory.inTransaction(session -> {
            Object[] actual = session.createNativeQuery(
                            "SELECT amount, largeAmount, fraction, integerAmount FROM decimal_precision_scale WHERE id = :id",
                            Object[].class)
                    .setParameter("id", entity.getId())
                    .getSingleResult();
            assertArrayEquals(new Object[]{entity.getAmount(), entity.getLargeAmount(), entity.getFraction(),
                    entity.getIntegerAmount()}, actual);
        });
    }

    private static DecimalEntity largeValues(long id) {
        return new DecimalEntity(id, new BigDecimal("1234567890123456.78"),
                new BigDecimal("1234567890123456789012345.1234567890"),
                new BigDecimal("0.12345678901234567890123456789012345"),
                new BigDecimal("12345678901234567890123456789012345"),
                new BigDecimal("1234567890123.123456789"), new BigInteger("1234567890123456789012"));
    }

    @Entity(name = "DecimalEntity")
    @Table(name = "decimal_precision_scale")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DecimalEntity {
        @Id
        private long id;
        @Column(precision = 18, scale = 2)
        private BigDecimal amount;
        @Column(precision = 35, scale = 10)
        private BigDecimal largeAmount;
        @Column(precision = 35, scale = 35)
        private BigDecimal fraction;
        @Column(precision = 35, scale = 0)
        private BigDecimal integerAmount;
        private BigDecimal defaultDecimal;
        private BigInteger defaultInteger;
    }
}
