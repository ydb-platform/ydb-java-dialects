package tech.ydb.liquibase.type;

import liquibase.change.core.LoadDataChange;
import liquibase.database.Database;
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

    @Override
    public String objectToSql(Object value, Database database) {
        if ((value == null) || "null".equalsIgnoreCase(value.toString())) {
            return "NULL";
        }

        return "TIMESTAMP('" + value + "')";
    }
}
