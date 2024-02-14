package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.AddUniqueConstraintStatement;
import tech.ydb.liquibase.exception.YdbMessageException;

/**
 * @author Kirill Kurdyukov
 */
public class AddUniqueConstraintGeneratorYdb extends BaseSqlGeneratorYdb<AddUniqueConstraintStatement> {

    @Override
    public ValidationErrors validate(
            AddUniqueConstraintStatement statement,
            Database database,
            SqlGeneratorChain<AddUniqueConstraintStatement> sqlGeneratorChain
    ) {
        ValidationErrors validationErrors = new ValidationErrors();

        validationErrors.addError(YdbMessageException.DOES_NOT_SUPPORT_UNIQUE_CONSTRAINT +
                YdbMessageException.badTableStrPointer(statement::getTableName));

        return validationErrors;
    }
}
