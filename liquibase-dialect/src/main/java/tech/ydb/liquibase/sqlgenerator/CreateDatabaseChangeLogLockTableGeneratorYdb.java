package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.statement.core.CreateDatabaseChangeLogLockTableStatement;
import liquibase.structure.core.Relation;
import liquibase.structure.core.Table;

/**
 * @author Kirill Kurdyukov
 */
public class CreateDatabaseChangeLogLockTableGeneratorYdb extends BaseSqlGeneratorYdb<CreateDatabaseChangeLogLockTableStatement> {

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
                                ) + " (ID " + DataTypeFactory.getInstance()
                                .fromDescription("int", database)
                                .toDatabaseDataType(database) + ", " +
                                "LOCKED " + DataTypeFactory.getInstance()
                                .fromDescription("boolean", database)
                                .toDatabaseDataType(database) + ", " +
                                "LOCKGRANTED " + DataTypeFactory.getInstance()
                                .fromDescription("datetime", database)
                                .toDatabaseDataType(database) + ", " +
                                "LOCKEDBY " + DataTypeFactory.getInstance()
                                .fromDescription("text", database)
                                .toDatabaseDataType(database) + ", " +
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
