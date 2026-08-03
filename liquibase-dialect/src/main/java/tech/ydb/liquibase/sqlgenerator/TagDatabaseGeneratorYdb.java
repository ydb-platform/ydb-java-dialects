package tech.ydb.liquibase.sqlgenerator;

import liquibase.database.Database;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.datatype.DataTypeFactory;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.TagDatabaseGenerator;
import liquibase.statement.core.TagDatabaseStatement;
import liquibase.structure.core.Column;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
public class TagDatabaseGeneratorYdb extends TagDatabaseGenerator {

    @Override
    public boolean supports(TagDatabaseStatement statement, Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public Sql[] generateSql(
            TagDatabaseStatement statement,
            Database database,
            SqlGeneratorChain sqlGeneratorChain
    ) {
        ObjectQuotingStrategy currentStrategy = database.getObjectQuotingStrategy();
        database.setObjectQuotingStrategy(ObjectQuotingStrategy.LEGACY);
        try {
            String tableNameEscaped = database.escapeTableName(
                    database.getLiquibaseCatalogName(),
                    database.getLiquibaseSchemaName(),
                    database.getDatabaseChangeLogTableName()
            );
            String idColumnEscaped = database.escapeObjectName("ID", Column.class);
            String authorColumnEscaped = database.escapeObjectName("AUTHOR", Column.class);
            String filenameColumnEscaped = database.escapeObjectName("FILENAME", Column.class);
            String tagColumnEscaped = database.escapeObjectName("TAG", Column.class);
            String orderColumnEscaped = database.escapeObjectName("ORDEREXECUTED", Column.class);
            String dateColumnEscaped = database.escapeObjectName("DATEEXECUTED", Column.class);
            String tagEscaped = DataTypeFactory.getInstance()
                    .fromObject(statement.getTag(), database)
                    .objectToSql(statement.getTag(), database);

            return new Sql[]{
                    new UnparsedSql(
                            "UPDATE " + tableNameEscaped +
                                    " ON SELECT * FROM (" +
                                    "SELECT " + idColumnEscaped + ", " + authorColumnEscaped + ", " +
                                    filenameColumnEscaped + ", Utf8(" + tagEscaped + ") AS " + tagColumnEscaped +
                                    " FROM " + tableNameEscaped +
                                    " ORDER BY " + dateColumnEscaped + " DESC, " + orderColumnEscaped + " DESC" +
                                    " LIMIT 1" +
                                    ")"
                    )
            };
        } finally {
            database.setObjectQuotingStrategy(currentStrategy);
        }
    }
}
