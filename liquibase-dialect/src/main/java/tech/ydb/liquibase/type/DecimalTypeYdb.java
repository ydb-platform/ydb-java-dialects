package tech.ydb.liquibase.type;

import liquibase.change.core.LoadDataChange;
import liquibase.database.Database;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Decimal",
        aliases = {"decimal, java.sql.Types.DECIMAL", "java.math.BigDecimal"},
        minParameters = 2,
        maxParameters = 2,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class DecimalTypeYdb extends BaseTypeYdb {

    @Override
    public boolean validate(Database database) {
        return super.validate(database) &&
                getParameters()[0].equals(22) && getParameters()[1].equals(9); // Fixed Decimal(22, 9)
    }

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        return new DatabaseDataType(getRawDefinition().toUpperCase());
    }

    @Override
    public LoadDataChange.LOAD_DATA_TYPE getLoadTypeName() {
        return LoadDataChange.LOAD_DATA_TYPE.NUMERIC;
    }

    @Override
    public String objectToSql(Object value, Database database) {
        return "CAST('" + value + "' AS DECIMAL(22,9))";
    }
}
