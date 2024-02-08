package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.statement.SqlStatement;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
abstract class BaseSqlGeneratorYdb<T extends SqlStatement> extends AbstractSqlGenerator<T> {

    @Override
    public boolean supports(T statement, Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public ValidationErrors validate(T statement, Database database, SqlGeneratorChain<T> sqlGeneratorChain) {
        return new ValidationErrors();
    }

    @Override
    public Sql[] generateSql(T statement, Database database, SqlGeneratorChain<T> sqlGeneratorChain) {
        return new Sql[0];
    }
}
