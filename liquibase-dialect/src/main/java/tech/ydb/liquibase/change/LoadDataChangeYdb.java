package tech.ydb.liquibase.change;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.exceptions.CsvMalformedLineException;
import liquibase.Scope;
import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.change.core.LoadDataChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.LiquibaseDataType;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.logging.Logger;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.RawSqlStatement;
import liquibase.util.csv.CSVReader;

import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
@DatabaseChange(
        name = "loadData",
        description = "Loads data from a CSV file into an existing table",
        priority = ChangeMetaData.PRIORITY_DATABASE,
        appliesTo = "table",
        since = "1.7"
)
public class LoadDataChangeYdb extends LoadDataChange {

    private static final int BATCH_SIZE = 20;
    private static final Logger LOG = Scope.getCurrentScope().getLog(LoadDataChange.class);

    protected final Map<String, LiquibaseDataType> columnToLiquibaseDataType = new LinkedHashMap<>();

    @Override
    public boolean supports(Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public SqlStatement[] generateStatements(Database database) {
        try (CSVReader reader = getCSVReader()) {
            if (reader == null) {
                throw new UnexpectedLiquibaseException("Unable to read file " + this.getFile());
            }

            String[] headers = reader.readNext();
            if (headers == null) {
                throw new UnexpectedLiquibaseException("Data file " + getFile() + " was empty");
            }

            JdbcConnection jdbcConnection = (JdbcConnection) database.getConnection();
            try (ResultSet resultSet = jdbcConnection.getMetaData().getColumns(null, null, tableName, null)) {
                while (resultSet.next()) {
                    columnToLiquibaseDataType.put(
                            resultSet.getString("COLUMN_NAME").toLowerCase(),
                            DataTypeFactory.getInstance()
                                    .fromDescription(resultSet.getString("TYPE_NAME"), database));
                }

                return getSqlStatements(database, reader, headers);
            }
        } catch (CsvMalformedLineException e) {
            throw new RuntimeException("Error parsing " + getRelativeTo() + " on line " + e.getLineNumber() + ": " + e.getMessage());
        } catch (UnexpectedLiquibaseException ule) {
            if ((getChangeSet() != null) && (getChangeSet().getFailOnError() != null) && !getChangeSet()
                    .getFailOnError()) {
                LOG.info("Changeset " + getChangeSet().toString(false) +
                        " failed, but failOnError was false.  Error: " + ule.getMessage());
                return SqlStatement.EMPTY_SQL_STATEMENT;
            } else {
                throw ule;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected SqlStatement[] getSqlStatements(Database database, CSVReader reader, String[] headers) throws IOException {
        List<SqlStatement> sqlStatements = new ArrayList<>();
        String[] line = reader.readNext();

        while (line != null) {
            StringBuilder yqlInsert = new StringBuilder();

            yqlInsert.append("INSERT INTO ")
                    .append(database.escapeTableName(getCatalogName(), getSchemaName(), tableName))
                    .append(" (");

            Iterator<String> iteratorColumnNames = Arrays.stream(headers).iterator();
            while (iteratorColumnNames.hasNext()) {
                yqlInsert.append(iteratorColumnNames.next());

                if (iteratorColumnNames.hasNext()) {
                    yqlInsert.append(", ");
                }
            }

            yqlInsert.append(") VALUES (");

            int batchI;
            boolean havePrev = false;

            for (batchI = 0; batchI < BATCH_SIZE && line != null; batchI++, line = reader.readNext()) {
                if (havePrev) {
                    yqlInsert.append("), (");
                }

                for (int i = 0; i < line.length; i++) {
                    String columnName = headers[i];

                    yqlInsert.append(
                            columnToLiquibaseDataType.getOrDefault(
                                    columnName,
                                    DataTypeFactory.getInstance().fromDescription("text", database)
                            ).objectToSql(line[i], database)
                    );

                    if (i < line.length - 1) {
                        yqlInsert.append(", ");
                    }
                }

                havePrev = true;
            }

            yqlInsert.append(")");

            sqlStatements.add(new RawSqlStatement(yqlInsert.toString()));
        }

        return sqlStatements.toArray(new SqlStatement[0]);
    }

    @Override
    protected InsertStatement createStatement(String catalogName, String schemaName, String tableName) {
        return super.createStatement(catalogName, schemaName, tableName);
    }
}
