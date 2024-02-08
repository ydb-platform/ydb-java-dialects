package tech.ydb.liquibase.type;

import liquibase.change.core.LoadDataChange;
import liquibase.database.Database;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Json", // It doesn't matter what type it is.
        aliases = {"Json", "JsonDocument", "json", "jsondocument", "Interval", "interval"},
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class OtherTypeYdb extends BaseTypeYdb {

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        return new DatabaseDataType(getRawDefinition());
    }

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.OTHER;
    }
}
