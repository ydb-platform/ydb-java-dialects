package tech.ydb.liquibase.type;

import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Float",
        aliases = {"java.sql.Types.FLOAT", "java.lang.Float", "real", "java.sql.Types.REAL"},
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)

public class FloatTypeYdb extends BaseTypeYdb {

    @Override
    protected String objectToSql(Object value) {
        return "CAST('" + value + "' AS FLOAT)";
    }
}
