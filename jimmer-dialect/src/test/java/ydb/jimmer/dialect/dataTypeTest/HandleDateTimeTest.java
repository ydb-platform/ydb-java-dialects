package ydb.jimmer.dialect.dataTypeTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Tests if the date/time mapping integrated in Jimmer affects
 * the value of the date/time objects. The changes in the values are compared
 * after being put through the ydb driver mapping:
 * 1. date/time -> driver
 * 2. date/time -> Jimmer -> driver
 */
public class HandleDateTimeTest {
    @Test
    public void javaClassMappingTest() {
        Instant instant = Instant.now();
        Assertions.assertEquals(instant, Timestamp.from(instant).toInstant());

        LocalDateTime localDateTime = LocalDateTime.parse("1970-01-01T00:00:00");
        Assertions.assertEquals(localDateTime,
                Instant.ofEpochMilli(
                        Timestamp.from((localDateTime).atZone(ZoneId.systemDefault()).toInstant())
                                .getTime()
                ).atZone(ZoneId.systemDefault()).toLocalDateTime()
        );

        LocalDate localDate = LocalDate.parse("1970-01-01");
        Assertions.assertEquals(localDate,
                Instant.ofEpochMilli(
                    java.sql.Date.valueOf(localDate)
                            .getTime()
                ).atZone(ZoneId.systemDefault()).toLocalDate()
        );

        // can't find how LocalTime is processed inside the driver
//        LocalTime localTime = LocalTime.parse("10:15");
//        Assertions.assertEquals(localTime, java.sql.Time.valueOf(localTime));

        java.util.Date utilDate = new java.util.Date(0);
        Assertions.assertEquals(utilDate, new Timestamp((utilDate).getTime()));
    }
}
