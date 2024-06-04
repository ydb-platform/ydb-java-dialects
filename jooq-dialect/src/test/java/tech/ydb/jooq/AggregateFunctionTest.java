package tech.ydb.jooq;

import jooq.generated.ydb.default_schema.tables.records.SeriesRecord;
import org.jooq.types.ULong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static jooq.generated.ydb.default_schema.Tables.*;
import static org.jooq.impl.DSL.asterisk;
import static org.junit.jupiter.api.Assertions.*;
import static tech.ydb.jooq.YdbFunction.*;

public class AggregateFunctionTest extends BaseTest {

    @BeforeEach
    public void beforeEach() {
        insertExampleSeries();
        insertExampleDatetime();
    }

    @Test
    public void testCount() {
        ULong result = dsl.select(count(asterisk()))
                .from(SERIES)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertEquals(3, result.intValue());

        result = dsl.select(countDistinct(SERIES.SERIES_INFO))
                .from(SERIES)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertEquals(3, result.intValue());

        dsl.insertInto(SERIES)
                .set(new SeriesRecord(ULong.valueOf(4), "Series One", "Info One", ULong.valueOf(20220101)))
                .execute();

        result = dsl.select(countDistinct(SERIES.SERIES_INFO))
                .from(SERIES)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertEquals(3, result.intValue());

        result = dsl.select(countDistinct(SERIES.SERIES_ID))
                .from(SERIES)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertEquals(4, result.intValue());
    }

    @Test
    public void testMinMax() {
        ULong resultLong = dsl.select(min(SERIES.SERIES_ID))
                .from(SERIES)
                .fetchOne()
                .value1();

        assertNotNull(resultLong);
        assertEquals(1, resultLong.intValue());

        String result = dsl.select(max(SERIES.SERIES_INFO))
                .from(SERIES)
                .fetchOne()
                .value1();

        assertEquals("Info Two", result);

        resultLong = dsl.select(maxDistinct(SERIES.RELEASE_DATE))
                .from(SERIES)
                .fetchOne()
                .value1();

        assertNotNull(resultLong);
        assertEquals(20220103, resultLong.intValue());
    }

    @Test
    public void testSum() {
        ULong resultULong = dsl.select(sumUnsigned(SERIES.SERIES_ID))
                .from(SERIES)
                .fetchOne()
                .value1();

        assertNotNull(resultULong);
        assertEquals(6, resultULong.intValue());

        Long resultLong = dsl.select(sumSigned(DATE_TABLE.INT_COL))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(resultLong);
        assertEquals(9, resultLong.intValue());

        Duration resultDuration = dsl.select(sumInterval(DATE_TABLE.INTERVAL))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertEquals(Duration.of(6, ChronoUnit.HOURS), resultDuration);

        resultULong = dsl.select(sumUnsignedDistinct(SERIES.SERIES_ID))
                .from(SERIES)
                .fetchOne()
                .value1();

        assertNotNull(resultULong);
        assertEquals(6, resultULong.intValue());


        BigDecimal resultDecimal = dsl.select(sumDecimal(DATE_TABLE.BIG))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(resultDecimal);
        assertEquals(6., resultDecimal.doubleValue());
    }

    @Test
    public void testAvg() {
        Double resultDouble = dsl.select(avgDouble(DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(resultDouble);
        assertTrue(resultDouble - 0.2 < 0.001);

        BigDecimal resultDecimal = dsl.select(avgDecimal(DATE_TABLE.BIG))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(resultDecimal);
        assertEquals(2., resultDecimal.doubleValue());

        Duration resultDuration = dsl.select(avgIntervalDistinct(DATE_TABLE.INTERVAL))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(resultDuration);
        assertEquals(Duration.of(2, ChronoUnit.HOURS), resultDuration);
    }

    @Test
    public void testSumIf() {
        Long resultLong = dsl.select(sumIfSigned(DATE_TABLE.INT_COL, DATE_TABLE.BIG.gt(BigDecimal.ONE)))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertEquals(7, resultLong);
    }

    @Test
    public void testAvgIf() {
        Double resultDouble = dsl.select(avgIf(DATE_TABLE.PERCENT, DATE_TABLE.BIG.gt(BigDecimal.ONE)))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertEquals(0.25, resultDouble);
    }

    @Test
    public void testSome() {
        LocalDate resultDate = dsl.select(some(DATE_TABLE.DATE))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(resultDate);
    }

    @Test
    public void testCountDistinctEstimate() {
        ULong resultCount = dsl.select(countDistinctEstimate(DATE_TABLE.DATE))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(resultCount);
        assertEquals(2, resultCount.intValue());

        resultCount = dsl.select(hyperLogLog(DATE_TABLE.DATE))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(resultCount);
        assertEquals(2, resultCount.intValue());
    }

    @Test
    public void testMaxMinBy() {
        Duration resultDuration = dsl.select(maxBy(DATE_TABLE.INTERVAL, DATE_TABLE.BIG))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertEquals(Duration.of(3, ChronoUnit.HOURS), resultDuration);

        resultDuration = dsl.select(minBy(DATE_TABLE.INTERVAL, DATE_TABLE.BIG))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertEquals(Duration.of(1, ChronoUnit.HOURS), resultDuration);
    }

    @Test
    public void testStdDev() {
        Double result = dsl.select(stdDev(DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.1 - result < 0.0001);

        result = dsl.select(stdDevSamp(DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.1 - result < 0.0001);

        result = dsl.select(populationStdDev(DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.0816 - result < 0.0001);

        result = dsl.select(stdDevPopulation(DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.0816 - result < 0.0001);

        result = dsl.select(stdDevPopulationDistinct(DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.0816 - result < 0.0001);
    }

    @Test
    public void testVariance() {
        Double result = dsl.select(variance(DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.01 - result < 0.0001);

        result = dsl.select(varianceSample(DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.01 - result < 0.0001);

        result = dsl.select(populationVariance(DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.0066 - result < 0.0001);

        result = dsl.select(variancePopulation(DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.0066 - result < 0.0001);

        result = dsl.select(variancePopulationDistinct(DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.0066 - result < 0.0001);
    }

    @Test
    public void testCovariance() {
        Double result = dsl.select(covariance(DATE_TABLE.PERCENT, DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.01 - result < 0.0001);

        result = dsl.select(covarianceSample(DATE_TABLE.PERCENT, DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.01 - result < 0.0001);

        result = dsl.select(covariancePopulation(DATE_TABLE.PERCENT, DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertTrue(0.0066 - result < 0.0001);
    }

    @Test
    public void testCorrelation() {
        Double result = dsl.select(correlation(DATE_TABLE.PERCENT, DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertEquals(1., result);
    }

    @Test
    public void testPercentile() {
        Double result = dsl.select(percentile(DATE_TABLE.PERCENT, 0.5))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertEquals(0.2, result);

        result = dsl.select(median(DATE_TABLE.PERCENT))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertNotNull(result);
        assertEquals(0.2, result);
    }

    @Test
    public void testBoolOps() {
        Boolean result = dsl.select(boolAnd(DATE_TABLE.INT_COL.eq(2)))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertEquals(false, result);

        result = dsl.select(boolOr(DATE_TABLE.INT_COL.eq(2)))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertEquals(true, result);

        result = dsl.select(boolXor(DATE_TABLE.INT_COL.eq(2)))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();

        assertEquals(true, result);
    }

    @Test
    public void testBitOps() {
        ULong result = dsl.select(bitAnd(DATE_TABLE.ID))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();
        assertNotNull(result);
        assertEquals(0, result.intValue());

        result = dsl.select(bitOr(DATE_TABLE.ID))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();
        assertNotNull(result);
        assertEquals(3, result.intValue());

        result = dsl.select(bitXor(DATE_TABLE.ID))
                .from(DATE_TABLE)
                .fetchOne()
                .value1();
        assertNotNull(result);
        assertEquals(0, result.intValue());
    }

    private void insertExampleSeries() {
        dsl.insertInto(SERIES)
                .set(getExampleRecords())
                .execute();
    }

    private void insertExampleDatetime() {
        dsl.insertInto(DATE_TABLE)
                .set(getExampleDateRecords())
                .execute();
    }
}
