package tech.ydb.liquibase.type;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Datetime",
        aliases = {
                "java.util.Date", "time", "java.sql.Types.TIME", "java.sql.Time",
                "timetz", "java.sql.Types.TIME_WITH_TIMEZONE"
        },
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class TimeTypeYdb extends BaseTypeYdb {

    @Override
    protected String objectToSql(Object value) {
        return "DATETIME('" + LocalDateTime.parse(value.toString())
                .atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_INSTANT) + "')";
    }
}
