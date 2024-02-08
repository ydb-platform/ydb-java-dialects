package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.AddForeignKeyConstraintStatement;
import tech.ydb.liquibase.exception.YdbMessageException;

/**
 * @author Kirill Kurdyukov
 */
public class AddForeignKeyConstraintGeneratorYdb extends BaseSqlGeneratorYdb<AddForeignKeyConstraintStatement> {

    @Override
    public ValidationErrors validate(
            AddForeignKeyConstraintStatement statement,
            Database database,
            SqlGeneratorChain<AddForeignKeyConstraintStatement> sqlGeneratorChain
    ) {
        ValidationErrors validationErrors = new ValidationErrors();

        validationErrors.addError(YdbMessageException.DOES_NOT_SUPPORT_FOREIGN_KEY_CONSTRAINTS +
                YdbMessageException.badTableStrPointer(statement::getBaseTableName));

        return validationErrors;
    }
}
