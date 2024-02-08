package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.CreateViewStatement;
import tech.ydb.liquibase.exception.YdbMessageException;

/**
 * @author Kirill Kurdyukov
 */
public class CreateViewGeneratorYdb extends BaseSqlGeneratorYdb<CreateViewStatement> {

    @Override
    public ValidationErrors validate(
            CreateViewStatement statement,
            Database database,
            SqlGeneratorChain<CreateViewStatement> sqlGeneratorChain
    ) {
        return YdbMessageException.ydbDoesNotSupportStatement(statement);
    }
}
