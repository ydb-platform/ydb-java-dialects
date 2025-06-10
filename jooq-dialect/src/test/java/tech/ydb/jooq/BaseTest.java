package tech.ydb.jooq;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

import jooq.generated.ydb.default_schema.tables.records.DateTableRecord;
import jooq.generated.ydb.default_schema.tables.records.SeriesRecord;
import org.jooq.CreateTableElementListStep;
import org.jooq.Table;
import org.jooq.types.ULong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import tech.ydb.test.junit5.YdbHelperExtension;

public abstract class BaseTest {

    protected static List<SeriesRecord> getExampleRecords() {
        return List.of(
                new SeriesRecord(ULong.valueOf(1), "Series One", "Info One", ULong.valueOf(20220101)),
                new SeriesRecord(ULong.valueOf(2), "Series Two", "Info Two", ULong.valueOf(20220102)),
                new SeriesRecord(ULong.valueOf(3), "Series Three", "Info Three", ULong.valueOf(20220103))
        );
    }

    protected static List<DateTableRecord> getExampleDateRecords() {
        LocalDate date = LocalDate.of(2024, 4, 1);
        LocalDate date32 = LocalDate.of(1024, 4, 1);
        LocalDateTime datetime = LocalDateTime.of(date, LocalTime.of(13, 29, 30));
        LocalDateTime datetime64 = LocalDateTime.of(date32, LocalTime.of(13, 29, 30));
        Instant instant = datetime.toInstant(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS);
        Instant instant64 = datetime64.toInstant(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS);
        Duration duration = Duration.of(1, ChronoUnit.HOURS);
        Duration duration64 = Duration.of(-1, ChronoUnit.HOURS);

        return List.of(
                new DateTableRecord(ULong.valueOf(1), 2, 0.1, new BigDecimal("1.000000000"),
                        date, datetime, instant, duration, date32, datetime64, instant64, duration64),
                new DateTableRecord(
                        ULong.valueOf(2),
                        3,
                        0.2,
                        new BigDecimal("2.000000000"),
                        date.plusDays(1),
                        datetime.plusDays(1),
                        instant.plus(1, ChronoUnit.DAYS),
                        duration.plus(1, ChronoUnit.HOURS),
                        date32.plusDays(1),
                        datetime64.plusDays(1),
                        instant64.plus(1, ChronoUnit.DAYS),
                        duration64.plus(1, ChronoUnit.HOURS)
                ),
                new DateTableRecord(
                        ULong.valueOf(3),
                        4,
                        0.3,
                        new BigDecimal("3.000000000"),
                        date.plusDays(1),
                        datetime.plusDays(2),
                        instant.plus(2, ChronoUnit.DAYS),
                        duration.plus(2, ChronoUnit.HOURS),
                        date32.plusDays(1),
                        datetime64.plusDays(2),
                        instant64.plus(2, ChronoUnit.DAYS),
                        duration64.plus(2, ChronoUnit.HOURS)
                )
        );
    }

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    protected static CloseableYdbDSLContext dsl;

    @BeforeAll
    public static void beforeAll() {
        dsl = YDB.using(jdbcUrl());

        dsl.createTableIfNotExists("series")
                .column("series_id", YdbTypes.UINT64)
                .column("title", YdbTypes.UTF8)
                .column("series_info", YdbTypes.UTF8)
                .column("release_date", YdbTypes.UINT64)
                .primaryKey("series_id")
                .execute();

        dsl.createTableIfNotExists("seasons")
                .column("series_id", YdbTypes.UINT64)
                .column("season_id", YdbTypes.UINT64)
                .column("title", YdbTypes.UTF8)
                .column("first_aired", YdbTypes.UINT64)
                .column("last_aired", YdbTypes.UINT64)
                .primaryKey("series_id", "season_id")
                .execute();

        dsl.createTableIfNotExists("episodes")
                .column("series_id", YdbTypes.UINT64)
                .column("season_id", YdbTypes.UINT64)
                .column("episode_id", YdbTypes.UINT64)
                .column("title", YdbTypes.UTF8)
                .column("air_date", YdbTypes.UINT64)
                .primaryKey("series_id", "season_id", "episode_id")
                .execute();

        dsl.createTableIfNotExists("hard_table")
                .column("id", YdbTypes.STRING)
                .column("first", YdbTypes.JSON)
                .column("second", YdbTypes.JSONDOCUMENT)
                .column("third", YdbTypes.YSON)
                .column("uuid", YdbTypes.UUID)
                .primaryKey("id")
                .execute();

        dsl.createTableIfNotExists("date_table")
                .column("id", YdbTypes.UINT64)
                .column("int_col", YdbTypes.INT32)
                .column("percent", YdbTypes.DOUBLE)
                .column("big", YdbTypes.DECIMAL)
                .column("date", YdbTypes.DATE)
                .column("datetime", YdbTypes.DATETIME)
                .column("timestamp", YdbTypes.TIMESTAMP)
                .column("interval", YdbTypes.INTERVAL)
                .column("date32", YdbTypes.DATE32)
                .column("datetime64", YdbTypes.DATETIME64)
                .column("timestamp64", YdbTypes.TIMESTAMP64)
                .column("interval64", YdbTypes.INTERVAL64)
                .primaryKey("id")
                .execute();

        CreateTableElementListStep createQuery = dsl.createTableIfNotExists("numeric").column("id", YdbTypes.INT32);

        for (int i = 1; i <= 23; i++) {
            createQuery.column(Integer.toString(i), YdbTypes.INT32);
        }

        createQuery.primaryKey("id").execute();
    }

    @AfterAll
    public static void afterAll() {
        dsl.close();
    }

    @AfterEach
    public void afterEach() {
        List<Table<?>> tables = dsl.meta().getTables();

        for (Table<?> table : tables) {
            if (!table.getName().startsWith(".sys")) {
                dsl.deleteFrom(table).execute();
            }
        }
    }

    protected static String jdbcUrl() {
        StringBuilder jdbc = new StringBuilder("jdbc:ydb:")
                .append(ydb.useTls() ? "grpcs://" : "grpc://")
                .append(ydb.endpoint())
                .append(ydb.database());

        if (ydb.authToken() != null) {
            jdbc.append("?").append("token=").append(ydb.authToken());
        }

        return jdbc.toString();
    }
}
