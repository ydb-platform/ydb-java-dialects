package tech.ydb.jooq;

import java.util.List;

import jooq.generated.ydb.default_schema.tables.records.SeriesRecord;
import org.jooq.Result;
import org.jooq.types.ULong;
import org.junit.jupiter.api.Test;

import static jooq.generated.ydb.default_schema.Tables.SERIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BatchReplaceTest extends BaseTest {

    @Test
    public void testSimpleInsertViaBatchReplace() {
        SeriesRecord newRecord = new SeriesRecord();
        newRecord.setSeriesId(ULong.valueOf(1));
        newRecord.setTitle("New Series");
        newRecord.setSeriesInfo("Info about the new series");
        newRecord.setReleaseDate(ULong.valueOf(20220101));

        dsl.batchReplace(newRecord)
                .execute();

        Result<SeriesRecord> upsertedRecord = dsl.selectFrom(SERIES)
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(1)))
                .fetch();

        assertEquals(List.of(newRecord), upsertedRecord);
    }

    @Test
    public void testSimpleUpdateViaBatchReplace() {
        dsl.batchInsert(getExampleRecords()).execute();

        SeriesRecord replaceRecord = new SeriesRecord(ULong.valueOf(1), "Updated Series One", "Updated Info One",
                ULong.valueOf(20220201));

        dsl.batchReplace(replaceRecord)
                .execute();

        SeriesRecord updatedRecord = dsl.selectFrom(SERIES)
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(1)))
                .fetchOne();

        assertNotNull(updatedRecord);
        assertEquals("Updated Series One", updatedRecord.getTitle());
        assertEquals("Updated Info One", updatedRecord.getSeriesInfo());
        assertEquals(ULong.valueOf(20220201), updatedRecord.getReleaseDate());
    }

    @Test
    public void testMultipleInsertViaBatchReplace() {
        List<SeriesRecord> records = getExampleRecords();

        dsl.batchReplace(records)
                .execute();

        Result<SeriesRecord> upsertedRecords = dsl.selectFrom(SERIES).fetch();

        assertEquals(records, upsertedRecords);
    }

}
