package tech.ydb.liquibase.type;

import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Int16",
        aliases = {"java.sql.Types.SMALLINT", "int2", "smallserial", "smallint"},
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class SmallIntTypeYdb extends BaseTypeYdb {

    @Override
    protected String objectToSql(Object value) {
        return "CAST(" + value + " AS INT16)";
    }
}
