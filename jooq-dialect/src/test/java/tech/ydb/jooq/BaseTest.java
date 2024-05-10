package tech.ydb.jooq;

import java.util.List;

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

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    protected static CloseableYdbDSLContext dsl;

    @BeforeAll
    public static void beforeAll() {
        dsl = YDB.using(jdbcUrl());

        dsl.createTable("series")
                .column("series_id", YdbTypes.UINT64)
                .column("title", YdbTypes.UTF8)
                .column("series_info", YdbTypes.UTF8)
                .column("release_date", YdbTypes.UINT64)
                .primaryKey("series_id")
                .execute();

        dsl.createTable("seasons")
                .column("series_id", YdbTypes.UINT64)
                .column("season_id", YdbTypes.UINT64)
                .column("title", YdbTypes.UTF8)
                .column("first_aired", YdbTypes.UINT64)
                .column("last_aired", YdbTypes.UINT64)
                .primaryKey("series_id", "season_id")
                .execute();

        dsl.createTable("episodes")
                .column("series_id", YdbTypes.UINT64)
                .column("season_id", YdbTypes.UINT64)
                .column("episode_id", YdbTypes.UINT64)
                .column("title", YdbTypes.UTF8)
                .column("air_date", YdbTypes.UINT64)
                .primaryKey("series_id", "season_id", "episode_id")
                .execute();

        dsl.createTable("hard_table")
                .column("id", YdbTypes.TEXT)
                .column("first", YdbTypes.JSON)
                .column("second", YdbTypes.JSONDOCUMENT)
                .column("third", YdbTypes.YSON)
                .primaryKey("id")
                .execute();

        CreateTableElementListStep createQuery = dsl.createTable("numeric").column("id", YdbTypes.INT32);

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
