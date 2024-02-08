package tech.ydb.liquibase.type;

import liquibase.change.core.LoadDataChange;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Date",
        minParameters = 0,
        maxParameters = 0,
        aliases = {
                "datetime", "java.sql.Types.DATE", "java.util.Date",
                "smalldatetime", "datetime2"
        },
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class DateTypeYdb extends BaseTypeYdb {

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.DATE;
    }
}
