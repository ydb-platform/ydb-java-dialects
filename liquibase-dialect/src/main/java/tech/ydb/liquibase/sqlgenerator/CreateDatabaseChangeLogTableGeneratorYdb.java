package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AbstractSqlGenerator;
import liquibase.statement.core.CreateDatabaseChangeLogTableStatement;
import liquibase.structure.core.Relation;
import liquibase.structure.core.Table;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
public class CreateDatabaseChangeLogTableGeneratorYdb extends AbstractSqlGenerator<CreateDatabaseChangeLogTableStatement> {

    @Override
    public boolean supports(CreateDatabaseChangeLogTableStatement statement, Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public ValidationErrors validate(
            CreateDatabaseChangeLogTableStatement statement,
            Database database,
            SqlGeneratorChain<CreateDatabaseChangeLogTableStatement> sqlGeneratorChain
    ) {
        return new ValidationErrors();
    }

    @Override
    public Sql[] generateSql(
            CreateDatabaseChangeLogTableStatement statement,
            Database database,
            SqlGeneratorChain<CreateDatabaseChangeLogTableStatement> sqlGeneratorChain
    ) {
        return new Sql[]{
                new UnparsedSql(
                        "CREATE TABLE " +
                        database.escapeTableName(
                                database.getLiquibaseCatalogName(),
                                database.getLiquibaseSchemaName(),
                                database.getDatabaseChangeLogTableName()
                        ) + " (ID Text, " +
                        "AUTHOR Text, " +
                        "FILENAME Text, " +
                        "DATEEXECUTED " + DataTypeFactory.getInstance()
                        .fromDescription("datetime", database)
                        .toDatabaseDataType(database) + ", " +
                        "ORDEREXECUTED Int32, " +
                        "EXECTYPE Text, " +
                        "MD5SUM Text, " +
                        "DESCRIPTION Text, " +
                        "COMMENTS Text, " +
                        "TAG Text, " +
                        "LIQUIBASE Text, " +
                        "CONTEXTS Text, " +
                        "LABELS Text, " +
                        "DEPLOYMENT_ID Text, " +
                        "PRIMARY KEY(ID, AUTHOR, FILENAME))",
                        getAffectedTable(database)
                )
        };
    }

    private static Relation getAffectedTable(Database database) {
        return new Table().setName(database.getDatabaseChangeLogTableName())
                .setSchema(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName());
    }
}
