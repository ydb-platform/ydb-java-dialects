package tech.ydb.jooq;

import jooq.generated.ydb.default_schema.tables.Series;
import jooq.generated.ydb.default_schema.tables.records.SeriesRecord;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.types.ULong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static jooq.generated.ydb.default_schema.Tables.SERIES;
import static org.jooq.impl.DSL.count;
import static org.junit.jupiter.api.Assertions.*;

public class SelectTest extends BaseTest {
    private static final SeriesRecord FIRST = new SeriesRecord(ULong.valueOf(1), "title", "info", ULong.MIN);
    private static final SeriesRecord SECOND = new SeriesRecord(ULong.valueOf(2), "second title", "second info", ULong.MAX);

    @BeforeEach
    public void setUp() {
        dsl.insertInto(SERIES)
                .set(FIRST)
                .newRecord()
                .set(SECOND)
                .execute();
    }

    @Test
    public void selectAllFrom() {
        Result<SeriesRecord> series = dsl
                .selectFrom(SERIES)
                .fetch();

        assertEquals(List.of(FIRST, SECOND), series);
    }

    @Test
    public void selectFromWhereText() {
        Result<SeriesRecord> series = dsl
                .selectFrom(SERIES)
                .where(SERIES.TITLE.eq("title"))
                .fetch();

        assertEquals(List.of(FIRST), series);
    }

    @Test
    public void testUnsignedTypeHandling() {
        SeriesRecord result = dsl.selectFrom(SERIES)
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(2)))
                .fetch()
                .get(0);

        assertEquals(ULong.MAX, result.getReleaseDate());
    }

    @Test
    public void testOrderBy() {
        Result<SeriesRecord> series = dsl.selectFrom(SERIES)
                .orderBy(SERIES.SERIES_ID.desc())
                .fetch();

        assertEquals(SECOND, series.get(0));
        assertEquals(FIRST, series.get(1));
    }

    @Test
    public void testGroupByHavingEmpty() {
        Result<Record2<String, Integer>> result = dsl
                .select(SERIES.SERIES_INFO, count(SERIES.SERIES_ID))
                .from(SERIES)
                .groupBy(SERIES.SERIES_INFO)
                .having(count(SERIES.SERIES_INFO).gt(1))
                .fetch();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupByHavingAll() {
        Result<Record2<String, Integer>> result = dsl
                .select(SERIES.SERIES_INFO, count(SERIES.SERIES_ID))
                .from(SERIES)
                .groupBy(SERIES.SERIES_INFO)
                .having(count(SERIES.SERIES_INFO).gt(0))
                .fetch();

        assertEquals(2, result.size());
    }


    @Test
    public void testJoinOperation() {
        Series s1 = SERIES.as("s1");
        Series s2 = SERIES.as("s2");

        List<SeriesRecord> result = dsl
                .select(s1.SERIES_ID, s1.TITLE, s1.SERIES_INFO, s1.RELEASE_DATE)
                .from(s1)
                .join(s2).on(s1.SERIES_ID.eq(s2.SERIES_ID))
                .where(s1.TITLE.like("%title%"))
                .fetchInto(SeriesRecord.class);

        assertEquals(2, result.size(), "Expected duplicated results due to join on same table");
    }
}
