package tech.ydb.liquibase.type;

import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Double",
        aliases = {"java.sql.Types.DOUBLE", "java.lang.Double"},
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class DoubleTypeYdb extends BaseTypeYdb {

    @Override
    protected String objectToSql(Object value) {
        return value.toString();
    }
}
