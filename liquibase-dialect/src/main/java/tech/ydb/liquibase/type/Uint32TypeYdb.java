package tech.ydb.liquibase.type;

import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Uint32",
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class Uint32TypeYdb extends BaseTypeYdb {

    @Override
    protected String objectToSql(Object value) {
        return "CAST(" + value + " AS UINT32)";
    }
}
