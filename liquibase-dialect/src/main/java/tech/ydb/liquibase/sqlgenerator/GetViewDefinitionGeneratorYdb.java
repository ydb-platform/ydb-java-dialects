package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.GetViewDefinitionStatement;
import tech.ydb.liquibase.exception.YdbMessageException;

/**
 * @author Kirill Kurdyukov
 */
public class GetViewDefinitionGeneratorYdb extends BaseSqlGeneratorYdb<GetViewDefinitionStatement> {

    @Override
    public ValidationErrors validate(
            GetViewDefinitionStatement statement,
            Database database,
            SqlGeneratorChain<GetViewDefinitionStatement> sqlGeneratorChain
    ) {
        return YdbMessageException.ydbDoesNotSupportStatement(statement);
    }
}
