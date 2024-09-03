package tech.ydb.jooq;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.jooq.Field;
import org.jooq.types.UByte;
import org.jooq.types.UInteger;
import org.jooq.types.ULong;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static tech.ydb.jooq.YDB.val;
import static tech.ydb.jooq.YdbFunction.abs;
import static tech.ydb.jooq.YdbFunction.addTimezone;
import static tech.ydb.jooq.YdbFunction.assumeStrict;
import static tech.ydb.jooq.YdbFunction.byteAtUtf8;
import static tech.ydb.jooq.YdbFunction.clearBit;
import static tech.ydb.jooq.YdbFunction.coalesce;
import static tech.ydb.jooq.YdbFunction.currentTzDate;
import static tech.ydb.jooq.YdbFunction.currentTzDatetime;
import static tech.ydb.jooq.YdbFunction.currentTzTimestamp;
import static tech.ydb.jooq.YdbFunction.currentUtcDate;
import static tech.ydb.jooq.YdbFunction.currentUtcDatetime;
import static tech.ydb.jooq.YdbFunction.currentUtcTimestamp;
import static tech.ydb.jooq.YdbFunction.endsWithUtf8;
import static tech.ydb.jooq.YdbFunction.ensure;
import static tech.ydb.jooq.YdbFunction.find;
import static tech.ydb.jooq.YdbFunction.flipBit;
import static tech.ydb.jooq.YdbFunction.greatest;
import static tech.ydb.jooq.YdbFunction.if_;
import static tech.ydb.jooq.YdbFunction.just;
import static tech.ydb.jooq.YdbFunction.least;
import static tech.ydb.jooq.YdbFunction.len;
import static tech.ydb.jooq.YdbFunction.length;
import static tech.ydb.jooq.YdbFunction.likely;
import static tech.ydb.jooq.YdbFunction.maxOf;
import static tech.ydb.jooq.YdbFunction.minOf;
import static tech.ydb.jooq.YdbFunction.nanvl;
import static tech.ydb.jooq.YdbFunction.pickle;
import static tech.ydb.jooq.YdbFunction.rFind;
import static tech.ydb.jooq.YdbFunction.random;
import static tech.ydb.jooq.YdbFunction.randomNumber;
import static tech.ydb.jooq.YdbFunction.randomUuid;
import static tech.ydb.jooq.YdbFunction.setBit;
import static tech.ydb.jooq.YdbFunction.stablePickle;
import static tech.ydb.jooq.YdbFunction.startsWithUtf8;
import static tech.ydb.jooq.YdbFunction.substring;
import static tech.ydb.jooq.YdbFunction.testBit;
import static tech.ydb.jooq.YdbFunction.toBytes;
import static tech.ydb.jooq.YdbFunction.unwrap;

public class FunctionTest extends BaseTest {

    @Test
    public void testCoalesce() {
        String notNullString = "not null string";
        String result = runFunction(coalesce(val((String) null), val(notNullString)));

        assertEquals(notNullString, result);
    }

    @Test
    public void testLength() {
        String str = "foo";
        UInteger result = runFunction(length(val(str)));

        assertEquals(3, result.intValue());

        result = runFunction(len(val(str)));

        assertEquals(3, result.intValue());
    }

    @Test
    public void testSubstring() {
        byte[] bytes = "abcdefg".getBytes();
        Field<byte[]> str = val(bytes);

        String result = new String(runFunction(substring(str, 3, 1)));
        assertEquals("d", result);

        result = new String(runFunction(substring(str, 3)));
        assertEquals("defg", result);

        result = new String(runFunction(substring(str, null, UInteger.valueOf(3))));
        assertEquals("abc", result);
    }

    @Test
    public void testFind() {
        Field<String> str = val("abcdefg_abcdefg");

        UInteger result = runFunction(find(str, "abc"));
        assertEquals(0, result.intValue());

        result = runFunction(find(str, "abc", 1));
        assertEquals(8, result.intValue());

        result = runFunction(find(str, "de", 2));
        assertEquals(3, result.intValue());
    }

    @Test
    public void testRFind() {
        Field<String> str = val("abcdefg_abcdefg");

        UInteger result = runFunction(rFind(str, "bcd"));
        assertEquals(9, result.intValue());

        result = runFunction(rFind(str, "bcd", 8));
        assertEquals(1, result.intValue());

        result = runFunction(rFind(str, "de", 10));
        assertEquals(3, result.intValue());
    }

    @Test
    public void testStartsEndsWith() {
        Field<String> str = val("abc_efg");

        Boolean result = runFunction(
                startsWithUtf8(str, "abc")
                        .and(endsWithUtf8(str, "efg"))
        );
        assertTrue(result);

        result = runFunction(
                startsWithUtf8(str, "efg")
                        .or(endsWithUtf8(str, "abc"))
        );
        assertFalse(result);

        result = runFunction(
                startsWithUtf8(str, (String) null)
        );
        assertNull(result);
    }

    @Test
    public void testIf() {
        Integer result = runFunction(if_(val(1).gt(0), 42, -1));

        assertNotNull(result);
        assertEquals(42, result);
    }

    @Test
    public void testNanvl() {
        Double result = runFunction(nanvl(val(42.), -1.));

        assertNotNull(result);
        assertEquals(42., result);

        result = runFunction(nanvl(val(Double.NaN), -1.));
        assertNotNull(result);
        assertEquals(-1., result);
    }

    @Test
    public void testRandom() {
        Double result = runFunction(random(42));

        assertNotNull(result);
        assertTrue(result >= 0 && result < 1);
    }

    @Test
    public void testRandomNumber() {
        ULong result = runFunction(randomNumber(42, "Hello, World!"));

        assertNotNull(result);
        assertEquals(1, result.toBigInteger().signum());
    }

    @Test
    public void testRandomUuid() {
        UUID result = runFunction(randomUuid(42, "Hello, World!", new byte[42]));

        assertNotNull(result);
    }

    @Test
    public void testCurrentUtc() {
        LocalDate currentDate = runFunction(currentUtcDate());
        assertNotNull(currentDate);

        LocalDateTime currentDatetime = runFunction(currentUtcDatetime());
        assertNotNull(currentDatetime);

        Instant currentTimestamp = runFunction(currentUtcTimestamp());
        assertNotNull(currentDatetime);
    }

    @Test
    public void testCurrentTz() {
        ZoneId zoneId = ZoneId.of("Europe/Moscow");

        ZonedDateTime currentDate = runFunction(currentTzDate(zoneId));
        assertNotNull(currentDate);

        ZonedDateTime currentDatetime = runFunction(currentTzDatetime(zoneId));
        assertNotNull(currentDatetime);

        ZonedDateTime currentTimestamp = runFunction(currentTzTimestamp(zoneId));
        assertNotNull(currentTimestamp);
    }

    @Test
    public void testAddTimezone() {
        ZoneId zoneId = ZoneId.of("Europe/Moscow");
        Instant now = Instant.ofEpochSecond(0);

        ZonedDateTime result = runFunction(addTimezone(now, zoneId));

        assertNotNull(result);
        assertEquals(now.atZone(zoneId), result);
    }

    @Test
    public void testMaxMinOf() {
        Integer result = runFunction(maxOf(val(1), val(42)));

        assertNotNull(result);
        assertEquals(42, result);

        result = runFunction(minOf(val(1), val(42)));

        assertNotNull(result);
        assertEquals(1, result);

        result = runFunction(greatest(val(1), val(42)));

        assertNotNull(result);
        assertEquals(42, result);

        result = runFunction(least(val(1), val(42)));

        assertNotNull(result);
        assertEquals(1, result);
    }

    @Test
    public void testEnsure() {
        Integer result = runFunction(ensure(val(42), val(1).gt(0)));

        assertNotNull(result);
        assertEquals(42, result);
    }

    @Test
    public void testAssumeStrict() {
        Integer result = runFunction(assumeStrict(val(42)));

        assertNotNull(result);
        assertEquals(42, result);
    }

    @Test
    public void testLikely() {
        Boolean result = runFunction(likely(val(1).gt(0)));

        assertNotNull(result);
        assertEquals(true, result);
    }

    @Test
    public void testToBytes() {
        byte[] result = runFunction(toBytes(val(42)));

        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals(42, result[0]);
    }

    @Test
    public void testByteAt() {
        Field<String> field = val("foo");
        UByte result = runFunction(byteAtUtf8(field, 0));

        assertNotNull(result);
        assertEquals(102, result.intValue());

        result = runFunction(byteAtUtf8(field, 1));
        assertNotNull(result);
        assertEquals(111, result.intValue());
    }

    @Test
    public void testBitOps() {
        Field<UInteger> field = val(UInteger.valueOf(42));

        Boolean resultTest = runFunction(testBit(field, 1));

        assertNotNull(resultTest);
        assertTrue(resultTest);

        UInteger resultClear = runFunction(clearBit(field, 1));

        assertNotNull(resultClear);
        assertEquals(40, resultClear.intValue());

        UInteger resultSet = runFunction(setBit(field, 0));

        assertNotNull(resultSet);
        assertEquals(43, resultSet.intValue());

        UInteger resultFlip = runFunction(flipBit(field, 1));

        assertNotNull(resultFlip);
        assertEquals(40, resultFlip.intValue());
    }

    @Test
    public void testAbs() {
        Integer result = runFunction(abs(val(-42)));

        assertNotNull(result);
        assertEquals(42, result);
    }

    @Test
    public void testOptional() {
        Field<Integer> field = val(42);

        Integer resultJust = runFunction(just(field));

        assertNotNull(resultJust);
        assertEquals(42, resultJust);

        Integer resultUnwrap = runFunction(unwrap(just(field)));

        assertNotNull(resultUnwrap);
        assertEquals(42, resultUnwrap);
    }

    @Test
    public void testPickle() {
        byte[] result = runFunction(pickle(val(42)));

        assertNotNull(result);
        assertEquals(2, result.length);

        result = runFunction(stablePickle(val(42)));

        assertNotNull(result);
        assertEquals(2, result.length);
    }

    private <T> T runFunction(Field<T> func) {
        return dsl.select(func).fetchOne().value1();
    }
}
