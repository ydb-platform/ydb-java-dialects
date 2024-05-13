package tech.ydb.jooq;

import jooq.generated.ydb.default_schema.tables.records.HardTableRecord;
import jooq.generated.ydb.default_schema.tables.records.SeriesRecord;
import org.jooq.JSON;
import org.jooq.JSONB;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.types.ULong;
import org.junit.jupiter.api.Test;
import tech.ydb.jooq.value.YSON;

import java.util.List;

import static jooq.generated.ydb.default_schema.Tables.HARD_TABLE;
import static jooq.generated.ydb.default_schema.Tables.SERIES;
import static org.junit.jupiter.api.Assertions.*;

public class InsertTest extends BaseTest {

    @Test
    public void testSimpleInsert() {
        SeriesRecord newRecord = new SeriesRecord();
        newRecord.setSeriesId(ULong.valueOf(1));
        newRecord.setTitle("New Series");
        newRecord.setSeriesInfo("Info about the new series");
        newRecord.setReleaseDate(ULong.valueOf(20220101));

        dsl.insertInto(SERIES)
                .set(newRecord)
                .execute();

        Result<SeriesRecord> insertedRecord = dsl.selectFrom(SERIES)
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(1)))
                .fetch();

        assertEquals(List.of(newRecord), insertedRecord);
    }

    @Test
    public void testBatchInsert() {
        List<SeriesRecord> records = List.of(
                new SeriesRecord(ULong.valueOf(1), "Series One", "Info One", ULong.valueOf(20220102)),
                new SeriesRecord(ULong.valueOf(2), "Series Two", "Info Two", ULong.valueOf(20220103)),
                new SeriesRecord(ULong.valueOf(3), "Series Three", "Info Three", ULong.valueOf(20220104))
        );

        dsl.batchInsert(records).execute();

        assertEquals(3, dsl.fetchCount(SERIES), "All three records should be inserted");
    }

    @Test
    public void testConditionalInsert() {
        SeriesRecord newRecord = new SeriesRecord();
        newRecord.setSeriesId(ULong.valueOf(1));
        newRecord.setTitle("Unique Series");
        newRecord.setSeriesInfo("Unique info");
        newRecord.setReleaseDate(ULong.valueOf(20220105));

        int insertCount = dsl.insertInto(SERIES)
                .set(newRecord)
                .execute();

        assertEquals(1, insertCount, "Record should be inserted as it is unique");

        assertThrows(DataAccessException.class, () -> dsl.insertInto(SERIES)
                .set(newRecord)
                .execute(), "Duplicate record should not be inserted");
    }

    @Test
    public void testInsertJsonTypes() {
        HardTableRecord record = new HardTableRecord();
        record.setId("test-id");
        record.setFirst(JSON.valueOf("{\"key\": \"value\"}"));
        record.setSecond(JSONB.valueOf("{\"list\": [1, 2, 3]}"));
        record.setThird(YSON.valueOf("{\"boolean\" = true}"));

        dsl.insertInto(HARD_TABLE)
                .set(record)
                .execute();

        Result<HardTableRecord> records = dsl.selectFrom(HARD_TABLE)
                .where(HARD_TABLE.ID.eq("test-id"))
                .fetch();

        assertEquals(List.of(record), records);
    }
}
