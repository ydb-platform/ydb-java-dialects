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
        name = "JsonDocument",
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class JsonDocumentTypeYdb extends LiquibaseDataType {

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        return new DatabaseDataType("JSONDOCUMENT");
    }

    @Override
    public String objectToSql(Object value, Database database) {
        if (value == null || "null".equalsIgnoreCase(value.toString())) {
            return "NULL";
        }

        return "CAST('" + value + "' AS JSONDOCUMENT)";
    }

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.OTHER;
    }
}
