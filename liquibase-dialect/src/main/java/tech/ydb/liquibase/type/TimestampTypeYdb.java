package tech.ydb.liquibase.type;

import liquibase.change.core.LoadDataChange;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Timestamp",
        aliases = {
                "timestamp", "java.sql.Types.TIMESTAMP", "java.sql.TIMESTAMP",
                "java.sql.Types.TIMESTAMP_WITH_TIMEZONE", "timestamptz"
        },
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class TimestampTypeYdb extends BaseTypeYdb {

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.DATE;
    }
}
