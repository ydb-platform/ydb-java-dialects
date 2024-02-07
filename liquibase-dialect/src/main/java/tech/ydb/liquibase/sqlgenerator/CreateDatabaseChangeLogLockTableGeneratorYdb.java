package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.statement.core.CreateDatabaseChangeLogLockTableStatement;
import liquibase.structure.core.Relation;
import liquibase.structure.core.Table;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
public class CreateDatabaseChangeLogLockTableGeneratorYdb extends AbstractSqlGenerator<CreateDatabaseChangeLogLockTableStatement> {

    @Override
    public boolean supports(CreateDatabaseChangeLogLockTableStatement statement, Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public ValidationErrors validate(CreateDatabaseChangeLogLockTableStatement statement, Database database, SqlGeneratorChain<CreateDatabaseChangeLogLockTableStatement> sqlGeneratorChain) {
        return new ValidationErrors();
    }

    @Override
    public Sql[] generateSql(
            CreateDatabaseChangeLogLockTableStatement statement,
            Database database,
            SqlGeneratorChain<CreateDatabaseChangeLogLockTableStatement> sqlGeneratorChain
    ) {
        return new Sql[]{
                new UnparsedSql(
                        "CREATE TABLE " +
                                database.escapeTableName(
                                        database.getLiquibaseCatalogName(),
                                        database.getLiquibaseSchemaName(),
                                        database.getDatabaseChangeLogLockTableName()
                                ) + " (ID Int32, " +
                                "LOCKED " + DataTypeFactory.getInstance()
                                .fromDescription("boolean", database) + ", " +
                                "LOCKGRANTED " + DataTypeFactory.getInstance()
                                .fromDescription("datetime", database)
                                .toDatabaseDataType(database) + ", " +
                                "LOCKEDBY Text, " +
                                "PRIMARY KEY(ID))",
                        getAffectedTable(database)
                )
        };
    }

    private static Relation getAffectedTable(Database database) {
        return new Table().setName(database.getDatabaseChangeLogTableName())
                .setSchema(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName());
    }
}
