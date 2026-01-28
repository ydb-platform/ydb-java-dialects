package tech.ydb.jooq;

import jooq.generated.ydb.default_schema.tables.records.SeriesRecord;
import org.jooq.Field;
import org.jooq.types.ULong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jooq.generated.ydb.default_schema.Tables.SERIES;
import static org.junit.jupiter.api.Assertions.*;

public class UpdateTest extends BaseTest {

    @BeforeEach
    public void setup() {
        dsl.batchInsert(
                new SeriesRecord(ULong.valueOf(1), "Series One", "Info One", ULong.valueOf(20220101)),
                new SeriesRecord(ULong.valueOf(2), "Series Two", "Info Two", ULong.valueOf(20220102)),
                new SeriesRecord(ULong.valueOf(3), "Series Three", "Info Three", ULong.valueOf(20220103))
        ).execute();
    }

    @Test
    public void testSimpleUpdate() {
        dsl.update(SERIES)
                .set(SERIES.TITLE.as(Field::getName), "Updated Series One")
                .set(SERIES.SERIES_INFO.as(Field::getName), "Updated Info One")
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(1)))
                .execute();

        SeriesRecord updatedRecord = dsl.selectFrom(SERIES)
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(1)))
                .fetchOne();

        assertNotNull(updatedRecord);
        assertEquals("Updated Series One", updatedRecord.getTitle());
        assertEquals("Updated Info One", updatedRecord.getSeriesInfo());
    }

    @Test
    public void testSimpleUpdateWithoutAliasing() {
        dsl.update(SERIES)
                .set(SERIES.TITLE, "Updated Series One")
                .set(SERIES.SERIES_INFO, "Updated Info One")
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(1)))
                .execute();

        SeriesRecord updatedRecord = dsl.selectFrom(SERIES)
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(1)))
                .fetchOne();

        assertNotNull(updatedRecord);
        assertEquals("Updated Series One", updatedRecord.getTitle());
        assertEquals("Updated Info One", updatedRecord.getSeriesInfo());
    }

    @Test
    public void testUpdateSetToField() {
        dsl.update(SERIES)
                .set(SERIES.TITLE, SERIES.TITLE.concat(" updated"))
                .set(SERIES.SERIES_INFO, SERIES.TITLE)
                .set(SERIES.RELEASE_DATE, SERIES.RELEASE_DATE.plus(5))
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(1)))
                .execute();

        SeriesRecord updatedRecord = dsl.selectFrom(SERIES)
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(1)))
                .fetchOne();

        assertNotNull(updatedRecord);
        assertEquals("Series One updated", updatedRecord.getTitle());
        assertEquals("Series One", updatedRecord.getSeriesInfo());
        assertEquals(ULong.valueOf(20220106), updatedRecord.getReleaseDate());
    }

    @Test
    public void testConditionalUpdate() {
        dsl.update(SERIES)
                .set(SERIES.TITLE.as(Field::getName), "Updated Early Release")
                .where(SERIES.RELEASE_DATE.lt(ULong.valueOf(20220103)))
                .execute();

        int count = dsl.fetchCount(SERIES, SERIES.TITLE.eq("Updated Early Release"));
        assertEquals(2, count, "Two records should have their titles updated");
    }
}
