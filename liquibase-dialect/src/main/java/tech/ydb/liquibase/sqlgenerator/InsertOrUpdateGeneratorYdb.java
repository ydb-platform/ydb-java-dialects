package tech.ydb.liquibase.sqlgenerator;

import java.util.Iterator;
import java.util.function.Function;
import liquibase.database.Database;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.InsertOrUpdateGenerator;
import liquibase.statement.core.InsertOrUpdateStatement;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
public class InsertOrUpdateGeneratorYdb extends InsertOrUpdateGenerator {

    @Override
    public boolean supports(InsertOrUpdateStatement statement, Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public Sql[] generateSql(
            InsertOrUpdateStatement insertOrUpdateStatement,
            Database database,
            SqlGeneratorChain sqlGeneratorChain
    ) {
        StringBuilder yqlUpsert = new StringBuilder();

        yqlUpsert.append("UPSERT INTO ").
                append(database.escapeTableName(
                        insertOrUpdateStatement.getCatalogName(),
                        insertOrUpdateStatement.getSchemaName(),
                        insertOrUpdateStatement.getTableName())
                )
                .append(" (");

        sequenceParams(insertOrUpdateStatement, yqlUpsert, Function.identity());

        yqlUpsert.append(") VALUES (");

        sequenceParams(insertOrUpdateStatement, yqlUpsert,
                columnName -> insertOrUpdateStatement.getColumnValues().get(columnName).toString());

        yqlUpsert.append(")");

        return new Sql[]{
                new UnparsedSql(yqlUpsert.toString(), getAffectedTable(insertOrUpdateStatement))
        };
    }

    private static void sequenceParams(
            InsertOrUpdateStatement insertOrUpdateStatement,
            StringBuilder yqlUpsert,
            Function<String, String> mapColumnName
    ) {
        Iterator<String> columnNameIterator = insertOrUpdateStatement.getColumnValues().keySet().iterator();
        while (columnNameIterator.hasNext()) {
            yqlUpsert.append(mapColumnName.apply(columnNameIterator.next()));

            if (columnNameIterator.hasNext()) {
                yqlUpsert.append(", ");
            }
        }
    }


    // <------------------ not used ------------------>
    @Override
    protected String getRecordCheck(
            InsertOrUpdateStatement insertOrUpdateStatement,
            Database database,
            String whereClause
    ) {
        return "";
    }

    @Override
    protected String getElse(Database database) {
        return "";
    }
}
