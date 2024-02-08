package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.DropPrimaryKeyStatement;
import tech.ydb.liquibase.exception.YdbMessageException;

/**
 * @author Kirill Kurdyukov
 */
public class DropPrimaryKeyGeneratorSql extends BaseSqlGeneratorYdb<DropPrimaryKeyStatement> {

    @Override
    public ValidationErrors validate(
            DropPrimaryKeyStatement statement,
            Database database,
            SqlGeneratorChain<DropPrimaryKeyStatement> sqlGeneratorChain
    ) {
        return YdbMessageException.ydbDoesNotSupportStatement(statement);
    }
}
