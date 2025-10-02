package tech.ydb.hibernate.types;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.hibernate.TestUtils;
import static tech.ydb.hibernate.TestUtils.basedConfiguration;
import static tech.ydb.hibernate.TestUtils.inTransaction;
import static tech.ydb.hibernate.TestUtils.jdbcUrl;
import tech.ydb.hibernate.dialect.code.YdbJdbcCode;
import tech.ydb.test.junit5.YdbHelperExtension;

/**
 * @author Kirill Kurdyukov
 */
public class YdbJdbcCodeTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @BeforeAll
    static void beforeAll() {
        TestUtils.SESSION_FACTORY = basedConfiguration()
                .addAnnotatedClass(YdbAllTypes.class)
                .setProperty(AvailableSettings.URL, jdbcUrl(ydb))
                .buildSessionFactory();
    }

    @Test
    public void integrationTest() {
        /*
         *     create table ydb_all_types (
         *         aDouble Double,
         *         aFloat Float,
         *         bool Bool,
         *         bytes Bytes,
         *         date Date,
         *         date32 Date32,
         *         datetime Datetime,
         *         datetime64 Datetime64,
         *         int16 Int16,
         *         int32 Int32,
         *         int64 Int64,
         *         int8 Int8,
         *         interval Interval,
         *         interval64 Interval64,
         *         json Json,
         *         jsonDocument JsonDocument,
         *         text Text,
         *         timestamp Timestamp,
         *         timestamp64 Timestamp64,
         *         uint16 Uint16,
         *         uint32 Uint32,
         *         uint64 Uint64,
         *         uint8 Uint8,
         *         uuid Uuid not null,
         *         yson Yson,
         *         primary key (uuid)
         *     )
         */
        var ydbAllEntity = new YdbAllTypes(
                UUID.randomUUID(),
                true, 1, 1, 1, 1, 1, 1, 1, 1,
                LocalDate.now(),
                LocalDateTime.of(2000, 1, 1, 1, 1, 1),
                Instant.now(),
                Duration.ofSeconds(1),
                LocalDate.ofYearDay(1000, 1),
                LocalDateTime.of(1000, 1, 1, 1, 1, 1),
                LocalDateTime.of(1000, 1, 1, 1, 1, 1).toInstant(ZoneOffset.UTC),
                Duration.ofSeconds(-1),
                "text",
                "{\"a\":\"a\"}",
                "{\"a\":\"a\"}",
                new byte[]{10, 20},
                "{a=1u}".getBytes(StandardCharsets.UTF_8),
                1.1f, 1.1
        );

        inTransaction(session -> session.persist(ydbAllEntity));
        inTransaction(session -> assertEquals(ydbAllEntity, session.find(YdbAllTypes.class, ydbAllEntity.getUuid())));

        var newYdbAllEntity = new YdbAllTypes(
                ydbAllEntity.getUuid(),
                false, 2, 2, 2, 2, 2, 2, 2, 2,
                LocalDate.now(),
                LocalDateTime.of(2000, 2, 2, 2, 2, 2),
                Instant.now(),
                Duration.ofSeconds(2),
                LocalDate.ofYearDay(1000, 2),
                LocalDateTime.of(1000, 2, 2, 2, 2, 2),
                LocalDateTime.of(1000, 2, 2, 2, 2, 2).toInstant(ZoneOffset.UTC),
                Duration.ofSeconds(-2),
                "new_text",
                "{\"b\":\"b\"}",
                "{\"b\":\"b\"}",
                new byte[]{20, 20},
                "{a=2u}".getBytes(StandardCharsets.UTF_8),
                2.2f, 2.2
        );
        inTransaction(session -> {
            session.createQuery(
                            """
                                    UPDATE YdbAllTypes e
                                    SET
                                      e.bool = :bool,
                                      e.uint8 = :uint8,
                                      e.int8 = :int8,
                                      e.uint16 = :uint16,
                                      e.int16 = :int16,
                                      e.uint32 = :uint32,
                                      e.int32 = :int32,
                                      e.uint64 = :uint64,
                                      e.int64 = :int64,
                                      e.date = :date,
                                      e.datetime = :datetime,
                                      e.timestamp = :timestamp,
                                      e.interval = :interval,
                                      e.date32 = :date32,
                                      e.datetime64 = :datetime64,
                                      e.timestamp64 = :timestamp64,
                                      e.interval64 = :interval64,
                                      e.text = :text,
                                      e.json = :json,
                                      e.jsonDocument = :jsonDocument,
                                      e.bytes = :bytes,
                                      e.yson = :yson,
                                      e.aFloat = :aFloat,
                                      e.aDouble = :aDouble
                                    """)
                    .setParameter("bool", newYdbAllEntity.isBool())
                    .setParameter("uint8", newYdbAllEntity.getUint8())
                    .setParameter("int8", newYdbAllEntity.getInt8())
                    .setParameter("uint16", newYdbAllEntity.getUint16())
                    .setParameter("int16", newYdbAllEntity.getInt16())
                    .setParameter("uint32", newYdbAllEntity.getUint32())
                    .setParameter("int32", newYdbAllEntity.getInt32())
                    .setParameter("uint64", newYdbAllEntity.getUint64())
                    .setParameter("int64", newYdbAllEntity.getInt64())
                    .setParameter("date", newYdbAllEntity.getDate())
                    .setParameter("datetime", newYdbAllEntity.getDatetime())
                    .setParameter("timestamp", newYdbAllEntity.getTimestamp())
                    .setParameter("interval", newYdbAllEntity.getInterval())
                    .setParameter("date32", newYdbAllEntity.getDate32())
                    .setParameter("datetime64", newYdbAllEntity.getDatetime64())
                    .setParameter("timestamp64", newYdbAllEntity.getTimestamp64())
                    .setParameter("interval64", newYdbAllEntity.getInterval64())
                    .setParameter("text", newYdbAllEntity.getText())
                    .setParameter("json", newYdbAllEntity.getJson())
                    .setParameter("jsonDocument", newYdbAllEntity.getJsonDocument())
                    .setParameter("bytes", newYdbAllEntity.getBytes())
                    .setParameter("yson", newYdbAllEntity.getYson())
                    .setParameter("aFloat", newYdbAllEntity.getAFloat())
                    .setParameter("aDouble", newYdbAllEntity.getADouble())
                    .executeUpdate();
            assertEquals(newYdbAllEntity, session.find(YdbAllTypes.class, newYdbAllEntity.getUuid()));
        });
    }

    @Entity(name = "YdbAllTypes")
    @Table(name = "ydb_all_types")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class YdbAllTypes {
        @Id
        @JdbcTypeCode(YdbJdbcCode.UUID)
        private UUID uuid;

        @Column
        @JdbcTypeCode(YdbJdbcCode.BOOL)
        private boolean bool;

        @Column
        @JdbcTypeCode(YdbJdbcCode.UINT8)
        private int uint8;

        @Column
        @JdbcTypeCode(YdbJdbcCode.INT8)
        private int int8;

        @Column
        @JdbcTypeCode(YdbJdbcCode.UINT16)
        private int uint16;

        @Column
        @JdbcTypeCode(YdbJdbcCode.INT16)
        private int int16;

        @Column
        @JdbcTypeCode(YdbJdbcCode.UINT32)
        private int uint32;

        @Column
        @JdbcTypeCode(YdbJdbcCode.INT32)
        private int int32;

        @Column
        @JdbcTypeCode(YdbJdbcCode.UINT64)
        private int uint64;

        @Column
        @JdbcTypeCode(YdbJdbcCode.INT64)
        private int int64;

        @Column
        @JdbcTypeCode(YdbJdbcCode.DATE)
        private LocalDate date;

        @Column
        @JdbcTypeCode(YdbJdbcCode.DATETIME)
        private LocalDateTime datetime;

        @Column
        @JdbcTypeCode(YdbJdbcCode.TIMESTAMP)
        private Instant timestamp;

        @Column
        @JdbcTypeCode(YdbJdbcCode.INTERVAL)
        private Duration interval;

        @Column
        @JdbcTypeCode(YdbJdbcCode.DATE_32)
        private LocalDate date32;

        @Column
        @JdbcTypeCode(YdbJdbcCode.DATETIME_64)
        private LocalDateTime datetime64;

        @Column
        @JdbcTypeCode(YdbJdbcCode.TIMESTAMP_64)
        private Instant timestamp64;

        @Column
        @JdbcTypeCode(YdbJdbcCode.INTERVAL_64)
        private Duration interval64;

        @Column
        @JdbcTypeCode(YdbJdbcCode.TEXT)
        private String text;

        @Column
        @JdbcTypeCode(YdbJdbcCode.JSON)
        private String json;

        @Column
        @JdbcTypeCode(YdbJdbcCode.JSON_DOCUMENT)
        private String jsonDocument;

        @Column
        @JdbcTypeCode(YdbJdbcCode.BYTES)
        private byte[] bytes;

        @Column
        @JdbcTypeCode(YdbJdbcCode.YSON)
        private byte[] yson;

        @Column
        @JdbcTypeCode(YdbJdbcCode.FLOAT)
        private float aFloat;

        @Column
        @JdbcTypeCode(YdbJdbcCode.DOUBLE)
        private double aDouble;
    }
}
