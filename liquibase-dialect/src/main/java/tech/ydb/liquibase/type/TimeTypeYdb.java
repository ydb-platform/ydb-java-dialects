package tech.ydb.liquibase.type;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import liquibase.change.core.LoadDataChange;
import liquibase.database.Database;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Datetime",
        aliases = {
                "datetime", "java.util.Date",
                "time", "java.sql.Types.TIME", "java.sql.Time",
                "timetz", "java.sql.Types.TIME_WITH_TIMEZONE"
        },
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class TimeTypeYdb extends BaseTypeYdb {

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.DATE;
    }

    @Override
    public String objectToSql(Object value, Database database) {
        if ((value == null) || "null".equalsIgnoreCase(value.toString())) {
            return "NULL";
        }

        return "DATETIME('" + LocalDateTime.parse(value.toString())
                .atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_INSTANT) + "')";
    }
}
