package tech.ydb.liquibase.type;

import liquibase.database.Database;
import liquibase.datatype.DataTypeInfo;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;
import liquibase.exception.UnexpectedLiquibaseException;

/**
 * @author Kirill Kurdyukov
 */
@DataTypeInfo(
        name = "Decimal",
        aliases = {"java.sql.Types.DECIMAL", "java.math.BigDecimal"},
        minParameters = 0,
        maxParameters = 2,
        priority = LiquibaseDataType.PRIORITY_DATABASE
)
public class DecimalTypeYdb extends BaseTypeYdb {

    @Override
    public boolean validate(Database database) {
        decimalType();
        return true;
    }

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        return new DatabaseDataType(decimalType());
    }

    @Override
    protected String objectToSql(Object value) {
        return "CAST('" + value + "' AS " + decimalType() + ")";
    }

    private String decimalType() {
        Object[] parameters = getParameters();
        if (parameters.length == 0) {
            return "DECIMAL(22,9)";
        }
        if (parameters.length != 2) {
            throw new UnexpectedLiquibaseException("YDB Decimal requires both precision and scale, or no parameters");
        }

        int precision;
        int scale;
        try {
            precision = Integer.parseInt(parameters[0].toString());
            scale = Integer.parseInt(parameters[1].toString());
        } catch (NumberFormatException e) {
            throw new UnexpectedLiquibaseException("YDB Decimal precision and scale must be integers", e);
        }
        if (precision < 1 || precision > 35 || scale < 0 || scale > precision) {
            throw new UnexpectedLiquibaseException(
                    "YDB Decimal requires 1 <= precision <= 35 and 0 <= scale <= precision");
        }
        return "DECIMAL(" + precision + "," + scale + ")";
    }
}
