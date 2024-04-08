package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.SelectFromDatabaseChangeLogGenerator;
import liquibase.statement.core.SelectFromDatabaseChangeLogStatement;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
public class SelectFromDatabaseChangeLogGeneratorYdb extends SelectFromDatabaseChangeLogGenerator {

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean supports(SelectFromDatabaseChangeLogStatement statement, Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public Sql[] generateSql(
            SelectFromDatabaseChangeLogStatement statement,
            Database database,
            SqlGeneratorChain sqlGeneratorChain
    ) {
        Sql[] sql = super.generateSql(statement, database, sqlGeneratorChain);

        return new Sql[] {new UnparsedSql("SCAN " + sql[0].toSql())};
    }
}
