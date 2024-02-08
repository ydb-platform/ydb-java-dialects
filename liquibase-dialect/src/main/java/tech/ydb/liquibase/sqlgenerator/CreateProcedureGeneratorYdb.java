package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.CreateProcedureStatement;
import tech.ydb.liquibase.exception.YdbMessageException;

/**
 * @author Kirill Kurdyukov
 */
public class CreateProcedureGeneratorYdb extends BaseSqlGeneratorYdb<CreateProcedureStatement> {

    @Override
    public ValidationErrors validate(
            CreateProcedureStatement statement,
            Database database,
            SqlGeneratorChain<CreateProcedureStatement> sqlGeneratorChain
    ) {
        return YdbMessageException.ydbDoesNotSupportStatement(statement);
    }
}
