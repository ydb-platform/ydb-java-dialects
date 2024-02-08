package tech.ydb.liquibase.type;

import liquibase.database.Database;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
abstract class BaseTypeYdb extends LiquibaseDataType {

    @Override
    public boolean supports(Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public DatabaseDataType toDatabaseDataType(Database database) {
        return new DatabaseDataType(getName());
    }
}
