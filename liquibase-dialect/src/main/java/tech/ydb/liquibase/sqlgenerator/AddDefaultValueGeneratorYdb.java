package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.AddDefaultValueStatement;
import tech.ydb.liquibase.exception.YdbMessageException;

/**
 * @author Kirill Kurdyukov
 */
public class AddDefaultValueGeneratorYdb extends BaseSqlGeneratorYdb<AddDefaultValueStatement> {

    @Override
    public ValidationErrors validate(
            AddDefaultValueStatement statement,
            Database database,
            SqlGeneratorChain<AddDefaultValueStatement> sqlGeneratorChain
    ) {
        ValidationErrors validationErrors = new ValidationErrors();

        validationErrors.addError(YdbMessageException.DOES_NOT_SUPPORT_DEFAULT_VALUE_CONSTRAINT +
                YdbMessageException.badTableStrPointer(statement::getTableName));

        return validationErrors;
    }
}
