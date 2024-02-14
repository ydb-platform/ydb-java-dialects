package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.CreateDatabaseChangeLogTableStatement;
import liquibase.structure.core.Relation;
import liquibase.structure.core.Table;

/**
 * @author Kirill Kurdyukov
 */
public class CreateDatabaseChangeLogTableGeneratorYdb extends BaseSqlGeneratorYdb<CreateDatabaseChangeLogTableStatement> {

    @Override
    public Sql[] generateSql(
            CreateDatabaseChangeLogTableStatement statement,
            Database database,
            SqlGeneratorChain<CreateDatabaseChangeLogTableStatement> sqlGeneratorChain
    ) {
        String textType = DataTypeFactory.getInstance()
                .fromDescription("text", database)
                .toDatabaseDataType(database)
                .toSql() + ", ";

        return new Sql[]{
                new UnparsedSql(
                        "CREATE TABLE " +
                                database.escapeTableName(
                                        database.getLiquibaseCatalogName(),
                                        database.getLiquibaseSchemaName(),
                                        database.getDatabaseChangeLogTableName()
                                ) + " (ID " + textType +
                                "AUTHOR " + textType +
                                "FILENAME " + textType +
                                "DATEEXECUTED " + DataTypeFactory.getInstance()
                                .fromDescription("datetime", database)
                                .toDatabaseDataType(database) + ", " +
                                "ORDEREXECUTED " + DataTypeFactory.getInstance()
                                .fromDescription("int", database)
                                .toDatabaseDataType(database) + ", " +
                                "EXECTYPE " + textType +
                                "MD5SUM " + textType +
                                "DESCRIPTION " + textType +
                                "COMMENTS " + textType +
                                "TAG " + textType +
                                "LIQUIBASE " + textType +
                                "CONTEXTS " + textType +
                                "LABELS " + textType +
                                "DEPLOYMENT_ID " + textType +
                                "PRIMARY KEY(ID, AUTHOR, FILENAME)" +
                                ")",
                        getAffectedTable(database)
                )
        };
    }

    private static Relation getAffectedTable(Database database) {
        return new Table().setName(database.getDatabaseChangeLogTableName())
                .setSchema(database.getLiquibaseCatalogName(), database.getLiquibaseSchemaName());
    }
}
