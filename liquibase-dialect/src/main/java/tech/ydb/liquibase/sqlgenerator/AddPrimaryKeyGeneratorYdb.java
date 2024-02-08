package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.AddPrimaryKeyStatement;
import tech.ydb.liquibase.exception.YdbMessageException;

/**
 * @author Kirill Kurdyukov
 */
public class AddPrimaryKeyGeneratorYdb extends BaseSqlGeneratorYdb<AddPrimaryKeyStatement> {

    @Override
    public ValidationErrors validate(
            AddPrimaryKeyStatement statement,
            Database database,
            SqlGeneratorChain<AddPrimaryKeyStatement> sqlGeneratorChain
    ) {
        ValidationErrors validationErrors = new ValidationErrors();

        validationErrors.addError(YdbMessageException.DOES_NOT_SUPPORT_PRIMARY_KEY_OUTSIDE_CREATE_TABLE +
                YdbMessageException.badTableStrPointer(statement::getTableName));

        return validationErrors;
    }
}
