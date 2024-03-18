package tech.ydb.liquibase.type;

import liquibase.change.core.LoadDataChange;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Bool",
        aliases = {
                "boolean", "java.sql.Types.BOOLEAN",
                "java.lang.Boolean", "bit", "bool",
        },
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class BoolTypeYdb extends BaseTypeYdb {

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.BOOLEAN;
    }

    @Override
    protected String objectToSql(Object value) {
        return value.toString();
    }
}
