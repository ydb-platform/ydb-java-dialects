package tech.ydb.jooq;

import jooq.generated.ydb.default_schema.tables.records.SeriesRecord;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.types.ULong;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static jooq.generated.ydb.default_schema.Tables.SERIES;
import static org.jooq.impl.DSL.*;
import static org.junit.jupiter.api.Assertions.*;

public class UpsertTest extends BaseTest {

    @Test
    public void testSimpleInsertViaUpsert() {
        SeriesRecord newRecord = new SeriesRecord();
        newRecord.setSeriesId(ULong.valueOf(1));
        newRecord.setTitle("New Series");
        newRecord.setSeriesInfo("Info about the new series");
        newRecord.setReleaseDate(ULong.valueOf(20220101));

        dsl.upsertInto(SERIES)
                .set(newRecord)
                .execute();

        Result<SeriesRecord> upsertedRecord = dsl.selectFrom(SERIES)
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(1)))
                .fetch();

        assertEquals(List.of(newRecord), upsertedRecord);
    }

    @Test
    public void testSimpleUpdateViaUpsert() {
        dsl.batchInsert(getExampleRecords()).execute();

        dsl.upsertInto(SERIES)
                .set(SERIES.SERIES_ID, ULong.valueOf(1))
                .set(SERIES.TITLE, "Updated Series One")
                .set(SERIES.SERIES_INFO, "Updated Info One")
                .execute();

        SeriesRecord updatedRecord = dsl.selectFrom(SERIES)
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(1)))
                .fetchOne();

        assertNotNull(updatedRecord);
        assertEquals("Updated Series One", updatedRecord.getTitle());
        assertEquals("Updated Info One", updatedRecord.getSeriesInfo());
    }

    @Test
    public void testMultipleInsertWithSetViaUpsert() {
        List<SeriesRecord> records = getExampleRecords();

        dsl.upsertInto(SERIES)
                .set(records.get(0))
                .newRecord()
                .set(records.get(1))
                .newRecord()
                .set(records.get(2))
                .execute();

        Result<SeriesRecord> upsertedRecords = dsl.selectFrom(SERIES).fetch();

        assertEquals(records, upsertedRecords);
    }

    @Test
    public void testMultipleInsertViaUpsert() {
        List<SeriesRecord> records = getExampleRecords();

        dsl.upsertInto(SERIES)
                .values(records.get(0).getSeriesId(), records.get(0).getTitle(), records.get(0).getSeriesInfo(), records.get(0).getReleaseDate())
                .values(records.get(1).getSeriesId(), records.get(1).getTitle(), records.get(1).getSeriesInfo(), records.get(1).getReleaseDate())
                .values(records.get(2).getSeriesId(), records.get(2).getTitle(), records.get(2).getSeriesInfo(), records.get(2).getReleaseDate())
                .execute();

        Result<SeriesRecord> upsertedRecords = dsl.selectFrom(SERIES).fetch();

        assertEquals(records, upsertedRecords);
    }

    @Test
    public void testMultipleInsertWithFullListingViaUpsert() {
        List<SeriesRecord> records = getExampleRecords();

        var upsertQuery = dsl.upsertInto(SERIES, SERIES.SERIES_ID, SERIES.TITLE, SERIES.SERIES_INFO, SERIES.RELEASE_DATE);

        for (SeriesRecord record : records) {
            upsertQuery = upsertQuery.values(record.getSeriesId(), record.getTitle(), record.getSeriesInfo(), record.getReleaseDate());
        }

        upsertQuery.execute();

        Result<SeriesRecord> upsertedRecords = dsl.selectFrom(SERIES).fetch();

        assertEquals(records, upsertedRecords);
    }

    @Test
    public void testUpsertNumericTable() {
        List<?> values = IntStream.rangeClosed(0, 23).boxed().toList();

        dsl.upsertInto(table("numeric"),
                        field(name("id"), YdbTypes.INT32),
                        field(name("1"), YdbTypes.INT32),
                        field(name("2"), YdbTypes.INT32),
                        field(name("3"), YdbTypes.INT32),
                        field(name("4"), YdbTypes.INT32),
                        field(name("5"), YdbTypes.INT32),
                        field(name("6"), YdbTypes.INT32),
                        field(name("7"), YdbTypes.INT32),
                        field(name("8"), YdbTypes.INT32),
                        field(name("9"), YdbTypes.INT32),
                        field(name("10"), YdbTypes.INT32),
                        field(name("11"), YdbTypes.INT32),
                        field(name("12"), YdbTypes.INT32),
                        field(name("13"), YdbTypes.INT32),
                        field(name("14"), YdbTypes.INT32),
                        field(name("15"), YdbTypes.INT32),
                        field(name("16"), YdbTypes.INT32),
                        field(name("17"), YdbTypes.INT32),
                        field(name("18"), YdbTypes.INT32),
                        field(name("19"), YdbTypes.INT32),
                        field(name("20"), YdbTypes.INT32),
                        field(name("21"), YdbTypes.INT32),
                        field(name("22"), YdbTypes.INT32),
                        field(name("23"), YdbTypes.INT32)
                )
                .values(values)
                .execute();

        Record record = dsl.selectFrom("numeric").fetchOne();

        assertNotNull(record);

        List<Object> result = record.intoList();

        assertEquals(values.size(), result.size());
        assertTrue(values.containsAll(result));
    }

    @Test
    public void testUpsertDiffByReplace() {
        List<SeriesRecord> records = getExampleRecords();

        dsl.upsertInto(SERIES)
                .set(records.get(0))
                .newRecord()
                .set(records.get(1))
                .newRecord()
                .set(records.get(2))
                .execute();

        dsl.upsertInto(SERIES, SERIES.SERIES_ID, SERIES.TITLE)
                .values(ULong.valueOf(1), "New title")
                .execute();

        SeriesRecord upsertedRecord = dsl.selectFrom(SERIES)
                .where(SERIES.SERIES_ID.eq(ULong.valueOf(1)))
                .fetchOne();

        assertEquals(records.get(0).getSeriesInfo(), upsertedRecord.getSeriesInfo());
        assertEquals(records.get(0).getReleaseDate(), upsertedRecord.getReleaseDate());
    }
}
