package tech.ydb.liquibase.sqlgenerator;

import java.util.Iterator;
import liquibase.database.Database;
import liquibase.datatype.DatabaseDataType;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.CreateTableGenerator;
import liquibase.statement.PrimaryKeyConstraint;
import liquibase.statement.core.CreateTableStatement;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
public class CreateTableGeneratorYdb extends CreateTableGenerator {

    @Override
    public boolean supports(CreateTableStatement statement, Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    /**
     * Example:
     * <code>
     * CREATE TABLE table_name (
     * column1 type1,
     * column2 type2 NOT NULL,
     * ...
     * columnN typeN,
     * INDEX index1_name GLOBAL ON ( column ),
     * INDEX index2_name GLOBAL ON ( column1, column2, ... ),
     * PRIMARY KEY ( column, ... )
     * )
     * </code>
     */
    @Override
    public Sql[] generateSql(
            CreateTableStatement statement,
            Database database,
            SqlGeneratorChain sqlGeneratorChain
    ) {
        StringBuilder yqlCreateTable = new StringBuilder();

        yqlCreateTable.append("CREATE TABLE ")
                .append(
                        database.escapeTableName(
                                statement.getCatalogName(),
                                statement.getSchemaName(),
                                statement.getTableName()
                        )
                )
                .append(" (");

        for (String columnName : statement.getColumns()) {
            DatabaseDataType columnType = statement.getColumnTypes().get(columnName)
                    .toDatabaseDataType(database);

            yqlCreateTable
                    .append(
                            database.escapeColumnName(
                                    statement.getCatalogName(),
                                    statement.getSchemaName(),
                                    statement.getTableName(),
                                    columnName
                            )
                    )
                    .append(" ")
                    .append(columnType);

            if (statement.getNotNullColumns().containsKey(columnName)) {
                yqlCreateTable.append(" NOT NULL");
            }

            yqlCreateTable.append(", ");
        }

        yqlCreateTable.append("PRIMARY KEY (");

        PrimaryKeyConstraint primaryKeyConstraint = statement.getPrimaryKeyConstraint();

        Iterator<String> columnNamesOfPrimaryKeyIterator = primaryKeyConstraint.getColumns().iterator();

        while (columnNamesOfPrimaryKeyIterator.hasNext()) {
            String columnName = columnNamesOfPrimaryKeyIterator.next();

            yqlCreateTable
                    .append(
                            database.escapeColumnName(
                                    statement.getCatalogName(),
                                    statement.getSchemaName(),
                                    statement.getTableName(),
                                    columnName
                            )
                    );

            if (columnNamesOfPrimaryKeyIterator.hasNext()) {
                yqlCreateTable.append(", ");
            }
        }
        yqlCreateTable.append(") )");

        return new Sql[]{new UnparsedSql(yqlCreateTable.toString(), getAffectedTable(statement))};
    }

    @Override
    public ValidationErrors validate(
            CreateTableStatement createTableStatement,
            Database database,
            SqlGeneratorChain sqlGeneratorChain
    ) {
        ValidationErrors errors = super.validate(createTableStatement, database, sqlGeneratorChain);

        if (createTableStatement.getPrimaryKeyConstraint() == null) {
            errors.addError("Table YDB needs to have a primary key. " +
                    badTableStrPointer(createTableStatement));
        }

        if (createTableStatement.getUniqueConstraints() != null) {
            errors.addError("YDB doesn't support UNIQUE CONSTRAINTS! " +
                    badTableStrPointer(createTableStatement));
        }

        if (createTableStatement.getAutoIncrementConstraints() != null) {
            errors.addError("YDB doesn't support AUTO INCREMENT! " +
                    badTableStrPointer(createTableStatement));
        }

        if (createTableStatement.getDefaultValues() != null) {
            errors.addError("YDB doesn't support DEFAULT VALUE! " +
                    badTableStrPointer(createTableStatement));
        }

        if (createTableStatement.getForeignKeyConstraints() != null) {
            errors.addError("YDB doesn't support FOREIGN KEY CONSTRAINTS! " +
                    badTableStrPointer(createTableStatement));
        }

        return errors;
    }

    private static String badTableStrPointer(CreateTableStatement createTableStatement) {
        return "[table_name = " + createTableStatement.getTableName() + "]";
    }
}
