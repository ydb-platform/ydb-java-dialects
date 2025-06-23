package tech.ydb.liquibase.change;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import liquibase.change.ChangeMetaData;
import liquibase.change.ColumnConfig;
import liquibase.change.DatabaseChange;
import liquibase.change.core.InsertDataChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.LiquibaseDataType;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.RawSqlStatement;

/**
 * @author Kirill Kurdyukov
 */
@DatabaseChange(
        name = "insert",
        description = "Inserts data into an existing table",
        priority = ChangeMetaData.PRIORITY_DATABASE,
        appliesTo = "table"
)
public class InsertDataChangeYdb extends InsertDataChange {

    @Override
    public SqlStatement[] generateStatements(Database database) {
        Map<String, LiquibaseDataType> columnToLiquibaseDataType = new HashMap<>();

        try {
            JdbcConnection jdbcConnection = (JdbcConnection) database.getConnection();
            try (ResultSet rs = jdbcConnection.getMetaData().getColumns(null, null, getTableName(), null)) {
                while (rs.next()) {
                    columnToLiquibaseDataType.put(
                            rs.getString("COLUMN_NAME").toLowerCase(),
                            DataTypeFactory.getInstance().fromDescription(rs.getString("TYPE_NAME"), database)
                    );
                }
            }

            StringBuilder yqlInsert = new StringBuilder()
                    .append("INSERT INTO ")
                    .append(database.escapeTableName(getCatalogName(), getSchemaName(), getTableName()))
                    .append(" (");

            Iterator<ColumnConfig> columnConfigIterator = getColumns().iterator();
            List<String> sqlValues = new ArrayList<>();

            while (columnConfigIterator.hasNext()) {
                ColumnConfig columnConfig = columnConfigIterator.next();

                yqlInsert.append(columnConfig.getName());

                if (columnConfigIterator.hasNext()) {
                    yqlInsert.append(", ");
                }

                sqlValues.add(
                        columnToLiquibaseDataType.getOrDefault(
                                columnConfig.getName(),
                                DataTypeFactory.getInstance().fromDescription("text", database)
                        ).objectToSql(columnConfig.getValueObject(), database)
                );
            }

            yqlInsert.append(") VALUES (");

            Iterator<String> sqlValueIterator = sqlValues.iterator();

            while (sqlValueIterator.hasNext()) {
                yqlInsert.append(sqlValueIterator.next());

                if (sqlValueIterator.hasNext()) {
                    yqlInsert.append(", ");
                }
            }
            yqlInsert.append(")");

            return new SqlStatement[]{new RawSqlStatement(yqlInsert.toString())};
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
