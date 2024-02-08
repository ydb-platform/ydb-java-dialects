package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.RenameColumnStatement;
import tech.ydb.liquibase.exception.YdbMessageException;

/**
 * @author Kirill Kurdyukov
 */
public class RenameColumnGeneratorYdb extends BaseSqlGeneratorYdb<RenameColumnStatement> {

    @Override
    public ValidationErrors validate(
            RenameColumnStatement statement,
            Database database,
            SqlGeneratorChain<RenameColumnStatement> sqlGeneratorChain
    ) {
        return YdbMessageException.ydbDoesNotSupportStatement(statement);
    }
}
