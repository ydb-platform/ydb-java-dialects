package tech.ydb.liquibase.type;

import liquibase.change.core.LoadDataChange;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Datetime",
        aliases = {
                "time", "java.sql.Types.TIME", "java.sql.Time",
                "timetz", "java.sql.Types.TIME_WITH_TIMEZONE"
        },
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class TimeTypeYdb extends CommonTypeYdb {

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.DATE;
    }
}
