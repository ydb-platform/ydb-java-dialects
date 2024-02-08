package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AddColumnGenerator;
import liquibase.statement.core.AddColumnStatement;
import tech.ydb.liquibase.database.YdbDatabase;
import tech.ydb.liquibase.exception.YdbMessageException;

/**
 * @author Kirill Kurdyukov
 */
public class AddColumnGeneratorYdb extends AddColumnGenerator {

    @Override
    public boolean supports(AddColumnStatement statement, Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public ValidationErrors validate(
            AddColumnStatement statement,
            Database database,
            SqlGeneratorChain sqlGeneratorChain
    ) {
        ValidationErrors validationErrors = super.validate(statement, database, sqlGeneratorChain);

        if (statement.isMultiple()) {
            for (AddColumnStatement addColumnStatement : statement.getColumns()) {
                checkAddColumnStatement(validationErrors, addColumnStatement);
            }
        } else {
            checkAddColumnStatement(validationErrors, statement);
        }

        return validationErrors;
    }

    private static void checkAddColumnStatement(ValidationErrors validationErrors, AddColumnStatement statement) {
        if (statement.isUnique()) {
            validationErrors.addError(YdbMessageException.DOES_NOT_SUPPORT_UNIQUE_CONSTRAINTS +
                    YdbMessageException.badTableStrPointer(statement::getTableName));
        }

        if (!statement.isNullable()) {
            validationErrors.addError(YdbMessageException.DOES_NOT_SUPPORT_NOT_NULL_CONSTRAINT +
                    YdbMessageException.badTableStrPointer(statement::getTableName));
        }

        if (statement.isAutoIncrement()) {
            validationErrors.addError(YdbMessageException.DOES_NOT_SUPPORT_AUTO_INCREMENT +
                    YdbMessageException.badTableStrPointer(statement::getTableName));
        }

        if (statement.isPrimaryKey()) {
            validationErrors.addError(YdbMessageException.DOES_NOT_SUPPORT_PRIMARY_KEY_OUTSIDE_CREATE_TABLE +
                    YdbMessageException.badTableStrPointer(statement::getTableName));
        }

        if (statement.getDefaultValue() != null) {
            validationErrors.addError(YdbMessageException.DOES_NOT_SUPPORT_DEFAULT_VALUE +
                    YdbMessageException.badTableStrPointer(statement::getTableName));
        }
    }
}
