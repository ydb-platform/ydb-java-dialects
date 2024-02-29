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
        aliases = {"Json", "JsonDocument", "Interval"},
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class OtherTypeYdb extends BaseTypeYdb {

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        return new DatabaseDataType(getRawDefinition().toUpperCase());
    }

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.OTHER;
    }

    @Override
    public String objectToSql(Object value, Database database) {
        if ((value == null) || "null".equalsIgnoreCase(value.toString())) {
            return "NULL";
        }

        if (getRawDefinition().equalsIgnoreCase("INTERVAL")) {
            return "CAST(" + value + " AS " + getRawDefinition() + ")";
        }

        return "CAST('" + value + "' AS " + getRawDefinition() + ")";
    }
}
