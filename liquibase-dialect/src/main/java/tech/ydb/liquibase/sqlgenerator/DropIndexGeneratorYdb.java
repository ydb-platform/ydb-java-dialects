package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.DropIndexGenerator;
import liquibase.statement.core.DropIndexStatement;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
public class DropIndexGeneratorYdb extends DropIndexGenerator {

    @Override
    public boolean supports(DropIndexStatement statement, Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }


    @Override
    public ValidationErrors validate(DropIndexStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        ValidationErrors validationErrors = super.validate(statement, database, sqlGeneratorChain);

        validationErrors.checkRequiredField("tableName", statement.getTableName());

        return validationErrors;
    }

    @Override
    public Sql[] generateSql(
            DropIndexStatement statement,
            Database database,
            SqlGeneratorChain sqlGeneratorChain
    ) {
        return new Sql[]{
                new UnparsedSql(
                        "ALTER TABLE " +
                                database.escapeTableName(
                                        statement.getTableCatalogName(),
                                        statement.getTableSchemaName(),
                                        statement.getTableName()
                                ) +
                                " DROP INDEX " +
                                database.escapeIndexName(
                                        statement.getTableCatalogName(),
                                        statement.getTableSchemaName(),
                                        statement.getIndexName()
                                ),
                        getAffectedIndex(statement)
                )
        };
    }
}
