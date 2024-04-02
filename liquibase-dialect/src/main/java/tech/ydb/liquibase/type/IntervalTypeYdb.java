package tech.ydb.liquibase.type;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Interval",
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class IntervalTypeYdb extends BaseTypeYdb {

    @Override
    public String objectToSql(Object value) {
        String valueStr = value.toString();
        Duration interval;

        if (valueStr.startsWith("PT")) {
            interval = Duration.parse(valueStr);
        } else {
            interval = Duration.parse("PT" + valueStr.toUpperCase());
        }

        return "CAST(" + TimeUnit.NANOSECONDS.toMicros(interval.toNanos()) + " AS INTERVAL)";
    }
}
