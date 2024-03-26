package tech.ydb.liquibase.type;

import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Int32",
        aliases = {"int", "integer", "java.sql.Types.INTEGER", "java.lang.Integer", "int4", "int32"},
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class IntTypeYdb extends BaseTypeYdb {
}
