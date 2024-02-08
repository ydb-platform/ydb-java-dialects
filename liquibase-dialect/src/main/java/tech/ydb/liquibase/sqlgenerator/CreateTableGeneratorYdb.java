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
import tech.ydb.liquibase.exception.YdbMessageException;
import static tech.ydb.liquibase.exception.YdbMessageException.DOES_NOT_SUPPORT_AUTO_INCREMENT;
import static tech.ydb.liquibase.exception.YdbMessageException.DOES_NOT_SUPPORT_DEFAULT_VALUE;
import static tech.ydb.liquibase.exception.YdbMessageException.DOES_NOT_SUPPORT_FOREIGN_KEY_CONSTRAINTS;
import static tech.ydb.liquibase.exception.YdbMessageException.DOES_NOT_SUPPORT_UNIQUE_CONSTRAINTS;

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
                    .append(columnType)
                    .append(", ");
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
            errors.addError("Table YDB needs to have a PRIMARY KEY. " +
                    YdbMessageException.badTableStrPointer(createTableStatement::getTableName));
        } else {
            if (createTableStatement.getPrimaryKeyConstraint().getConstraintName() != null) {
                errors.addWarning("YDB doesn't use PRIMARY KEY constraint name! " +
                        YdbMessageException.badTableStrPointer(createTableStatement::getTableName));
            }
        }

        if (createTableStatement.getUniqueConstraints() != null &&
                !createTableStatement.getUniqueConstraints().isEmpty()
        ) {
            errors.addError(DOES_NOT_SUPPORT_UNIQUE_CONSTRAINTS +
                    YdbMessageException.badTableStrPointer(createTableStatement::getTableName));
        }

        if (createTableStatement.getAutoIncrementConstraints() != null &&
                !createTableStatement.getAutoIncrementConstraints().isEmpty()
        ) {
            errors.addError(DOES_NOT_SUPPORT_AUTO_INCREMENT +
                    YdbMessageException.badTableStrPointer(createTableStatement::getTableName));
        }

        if (createTableStatement.getDefaultValues() != null &&
                !createTableStatement.getDefaultValues().isEmpty()
        ) {
            errors.addError(DOES_NOT_SUPPORT_DEFAULT_VALUE +
                    YdbMessageException.badTableStrPointer(createTableStatement::getTableName));
        }

        if (createTableStatement.getForeignKeyConstraints() != null &&
                !createTableStatement.getForeignKeyConstraints().isEmpty()
        ) {
            errors.addError(DOES_NOT_SUPPORT_FOREIGN_KEY_CONSTRAINTS +
                    YdbMessageException.badTableStrPointer(createTableStatement::getTableName));
        }

        return errors;
    }
}
