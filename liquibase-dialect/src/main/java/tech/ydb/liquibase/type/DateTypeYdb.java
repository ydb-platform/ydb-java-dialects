package tech.ydb.liquibase.type;

import liquibase.change.core.LoadDataChange;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Date",
        minParameters = 0,
        maxParameters = 0,
        aliases = {"java.sql.Types.DATE", "smalldatetime"},
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class DateTypeYdb extends BaseTypeYdb {

    @Override
    protected String objectToSql(Object value) {
        return "DATE('" + value + "')";
    }
}