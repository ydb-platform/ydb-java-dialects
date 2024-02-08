package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.RenameTableGenerator;
import liquibase.statement.core.RenameTableStatement;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
public class RenameTableGeneratorYdb extends RenameTableGenerator {

    @Override
    public boolean supports(RenameTableStatement statement, Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public Sql[] generateSql(RenameTableStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        return new Sql[]{
                new UnparsedSql(
                        "ALTER TABLE " +
                                database.escapeTableName(
                                        statement.getCatalogName(),
                                        statement.getSchemaName(),
                                        statement.getOldTableName()
                                ) + " RENAME TO " +
                                database.escapeTableName(
                                        statement.getCatalogName(),
                                        statement.getSchemaName(),
                                        statement.getNewTableName()
                                ),
                        getAffectedOldTable(statement),
                        getAffectedNewTable(statement)
                )
        };
    }
}
