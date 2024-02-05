package tech.ydb.liquibase.type;

import liquibase.change.core.LoadDataChange;
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
                "integer8", "bigserial", "serial8", "int8"
        },
        minParameters = 0,
        maxParameters = 0,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class LongTypeYdb extends CommonTypeYdb {

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.NUMERIC;
    }
}
