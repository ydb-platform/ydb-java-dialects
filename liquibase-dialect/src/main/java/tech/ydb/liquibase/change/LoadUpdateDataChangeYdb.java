package tech.ydb.liquibase.change;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.InsertOrUpdateStatement;
import liquibase.statement.core.InsertStatement;
import liquibase.util.csv.CSVReader;

/**
 * @author Kirill Kurdyukov
 */
@DatabaseChange(
        name = "loadUpdateData",
        description = "Loads or updates data from a CSV file into an existing table. Differs from loadData by " +
                "issuing a SQL batch that checks for the existence of a record. If found, the record is UPDATEd, " +
                "else the record is INSERTed. Also, generates DELETE statements for a rollback.\n" +
                "\n" +
                "A value of NULL in a cell will be converted to a database NULL rather than the string 'NULL'",
        priority = ChangeMetaData.PRIORITY_DATABASE,
        appliesTo = "table",
        since = "2.0"
)
public class LoadUpdateDataChangeYdb extends LoadDataChangeYdb {

    @Override
    protected boolean hasPreparedStatementsImplemented() {
        return false;
    }

    @Override
    public SqlStatement[] generateStatements(Database database) {
        return super.generateStatements(database);
    }

    protected SqlStatement[] getSqlStatements(Database database, CSVReader reader, String[] headers) throws IOException {
        String[] line;
        List<SqlStatement> sqlStatements = new ArrayList<>();

        while ((line = reader.readNext()) != null) {
            InsertStatement insertStatement = new InsertOrUpdateStatement(catalogName, schemaName, tableName, "");

            for (int i = 0; i < line.length; i++) {
                String columnName = headers[i];

                insertStatement.addColumnValue(
                        columnName,
                        columnToLiquibaseDataType.getOrDefault(
                                columnName,
                                DataTypeFactory.getInstance().fromDescription("text", database)
                        ).objectToSql(line[i], database)
                );
            }

            sqlStatements.add(insertStatement);
        }

        return sqlStatements.toArray(new SqlStatement[]{});
    }
}
