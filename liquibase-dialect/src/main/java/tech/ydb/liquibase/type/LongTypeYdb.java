package tech.ydb.liquibase.type;

import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Int64",
        aliases = {
                "bigint", "java.sql.Types.BIGINT",
                "java.math.BigInteger", "java.lang.Long",
                "integer8", "bigserial", "long",
        },
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class LongTypeYdb extends BaseTypeYdb {

    @Override
    protected String objectToSql(Object value) {
        return value.toString();
    }
}
