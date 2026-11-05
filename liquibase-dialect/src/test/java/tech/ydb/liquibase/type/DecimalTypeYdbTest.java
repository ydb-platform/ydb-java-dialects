package tech.ydb.liquibase.type;

import java.math.BigDecimal;
import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.LiquibaseDataType;
import liquibase.exception.UnexpectedLiquibaseException;
import org.junit.jupiter.api.Test;
import tech.ydb.liquibase.database.YdbDatabase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecimalTypeYdbTest {

    private final YdbDatabase database = new YdbDatabase();

    @Test
    void defaultParameters() {
        for (String name : new String[]{"decimal", "java.sql.Types.DECIMAL", "java.math.BigDecimal"}) {
            LiquibaseDataType type = DataTypeFactory.getInstance().fromDescription(name, database);
            assertTrue(type.validate(database));
            assertEquals("DECIMAL(22,9)", type.toDatabaseDataType(database).toString());
            assertEquals("CAST('1.23' AS DECIMAL(22,9))", type.objectToSql(new BigDecimal("1.23"), database));
        }
    }

    @Test
    void explicitParametersAndAliases() {
        for (String name : new String[]{"Decimal", "decimal", "java.sql.Types.DECIMAL", "java.math.BigDecimal"}) {
            LiquibaseDataType type = DataTypeFactory.getInstance().fromDescription(name + "(35, 10)", database);
            assertTrue(type.validate(database));
            assertEquals("DECIMAL(35,10)", type.toDatabaseDataType(database).toString());
            assertEquals("CAST('1234567890123456789012345.1234567890' AS DECIMAL(35,10))",
                    type.objectToSql("1234567890123456789012345.1234567890", database));
        }
    }

    @Test
    void parameterBoundaries() {
        for (String parameters : new String[]{"1,0", "1,1", "35,0", "35,35", "22,9"}) {
            LiquibaseDataType type = DataTypeFactory.getInstance().fromDescription("Decimal(" + parameters + ")", database);
            assertTrue(type.validate(database));
            assertEquals("DECIMAL(" + parameters + ")", type.toDatabaseDataType(database).toString());
        }
    }

    @Test
    void numericParameterObjects() {
        DecimalTypeYdb type = new DecimalTypeYdb();
        type.addParameter(18);
        type.addParameter(2);
        assertTrue(type.validate(database));
        assertEquals("DECIMAL(18,2)", type.toDatabaseDataType(database).toString());
    }

    @Test
    void invalidParametersAreRejectedDuringValidationAndConversion() {
        for (String parameters : new String[]{"10", "10,2,1", "0,0", "36,0", "-1,0", "10,-1", "10,11",
                "1.5,0", "10,1.5", "text,0", "10,text", "2147483648,0"}) {
            LiquibaseDataType type = DataTypeFactory.getInstance().fromDescription("Decimal(" + parameters + ")", database);
            assertThrows(UnexpectedLiquibaseException.class, () -> type.validate(database), parameters);
            assertThrows(UnexpectedLiquibaseException.class, () -> type.toDatabaseDataType(database), parameters);
            assertThrows(UnexpectedLiquibaseException.class, () -> type.objectToSql("1", database), parameters);
        }
    }

    @Test
    void invalidNumberRetainsCause() {
        LiquibaseDataType type = DataTypeFactory.getInstance().fromDescription("Decimal(text,0)", database);
        UnexpectedLiquibaseException failure = assertThrows(UnexpectedLiquibaseException.class,
                () -> type.toDatabaseDataType(database));
        assertInstanceOf(NumberFormatException.class, failure.getCause());
    }

    @Test
    void nullValues() {
        LiquibaseDataType type = DataTypeFactory.getInstance().fromDescription("Decimal(35,10)", database);
        assertEquals("NULL", type.objectToSql(null, database));
        assertEquals("NULL", type.objectToSql("null", database));
    }
}
