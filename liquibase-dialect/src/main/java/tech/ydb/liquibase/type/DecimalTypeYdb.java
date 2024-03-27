package tech.ydb.liquibase.type;

import liquibase.database.Database;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Decimal",
        aliases = {"java.sql.Types.DECIMAL", "java.math.BigDecimal"},
        minParameters = 2,
        maxParameters = 2,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class DecimalTypeYdb extends BaseTypeYdb {

    @Override
    public boolean validate(Database database) {
        return super.validate(database) &&
                getParameters()[0].equals(22) && getParameters()[1].equals(9); // Fixed Decimal(22,9)
    }

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        return new DatabaseDataType("DECIMAL(22,9)");
    }

    @Override
    protected String objectToSql(Object value) {
        return "CAST('" + value + "' AS DECIMAL(22,9))";
    }
}
