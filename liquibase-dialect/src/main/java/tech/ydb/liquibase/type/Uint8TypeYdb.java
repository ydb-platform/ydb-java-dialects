package tech.ydb.liquibase.type;

import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Uint8",
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class Uint8TypeYdb extends BaseTypeYdb {
}
