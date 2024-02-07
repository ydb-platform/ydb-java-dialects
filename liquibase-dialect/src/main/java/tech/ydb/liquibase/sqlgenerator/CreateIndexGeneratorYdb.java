package tech.ydb.liquibase.sqlgenerator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import liquibase.change.AddColumnConfig;
import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.CreateIndexGenerator;
import liquibase.statement.core.CreateIndexStatement;
import liquibase.structure.core.Index;
import liquibase.util.StringUtil;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
public class CreateIndexGeneratorYdb extends CreateIndexGenerator {

    @Override
    public boolean supports(CreateIndexStatement statement, Database database) {
        return database instanceof YdbDatabase;
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    /**
     * Generate a CREATE INDEX SQL statement for Yandex DB.
     *
     * @param statement         A CreateIndexStatement with the desired properties of the SQL to be generated
     * @param database          A database object (must be of YdbDatabase type, or we will error out)
     * @param sqlGeneratorChain The other generators in the current chain (ignored by this implementation)
     * @return An array with one entry containing the generated CREATE INDEX statement for YDB.
     */
    @Override
    public Sql[] generateSql(
            CreateIndexStatement statement,
            Database database,
            SqlGeneratorChain sqlGeneratorChain
    ) {
        List<String> associatedWith = StringUtil.splitAndTrim(statement.getAssociatedWith(), ",");
        if (associatedWith != null && (associatedWith.contains(Index.MARK_PRIMARY_KEY) ||
                associatedWith.contains(Index.MARK_UNIQUE_CONSTRAINT))
        ) {
            return EMPTY_SQL;
        }

        StringBuilder yqlCreateIndex = new StringBuilder();

        yqlCreateIndex.append("ALTER TABLE ")
                .append(
                        database.escapeTableName(
                                statement.getTableCatalogName(),
                                statement.getTableSchemaName(),
                                statement.getTableName()
                        )
                )
                .append(" ADD INDEX ")
                .append(
                        database.escapeIndexName(
                                statement.getTableCatalogName(),
                                statement.getTableSchemaName(),
                                statement.getIndexName()
                        )
                )
                .append(" GLOBAL ON (");

        Iterator<AddColumnConfig> iterator = Arrays.asList(statement.getColumns()).iterator();

        while (iterator.hasNext()) {
            AddColumnConfig column = iterator.next();

            yqlCreateIndex.append(
                    database.escapeColumnName(
                            statement.getTableCatalogName(),
                            statement.getTableSchemaName(),
                            statement.getTableName(),
                            column.getName()
                    )
            );

            if (iterator.hasNext()) {
                yqlCreateIndex.append(", ");
            }
        }
        yqlCreateIndex.append(")");

        return new Sql[]{new UnparsedSql(yqlCreateIndex.toString(), getAffectedIndex(statement))};
    }

    @Override
    public ValidationErrors validate(
            CreateIndexStatement createIndexStatement,
            Database database,
            SqlGeneratorChain sqlGeneratorChain
    ) {
        ValidationErrors errors = super.validate(createIndexStatement, database, sqlGeneratorChain);
        errors.checkRequiredField("name", createIndexStatement.getIndexName());

        if (createIndexStatement.isUnique()) {
            errors.addError("YDB doesn't support UNIQUE INDEX! " +
                    "[table name = " + createIndexStatement.getTableName() + ", " +
                    "index name = " + createIndexStatement.getIndexName() + "]");
        }

        for (AddColumnConfig column : createIndexStatement.getColumns()) {
            if (column.getDescending() != null && column.getDescending()) {
                errors.addError("YDB doesn't support descending column in index! " +
                        badColumnStrPointer(createIndexStatement, column));
            }

           if (column.getComputed() != null && column.getComputed()) {
               errors.addError("YDB doesn't support computed column in index! " +
                       badColumnStrPointer(createIndexStatement, column));
           }
        }

        return errors;
    }

    private static String badColumnStrPointer(CreateIndexStatement createIndexStatement, AddColumnConfig column) {
        return "[table name = " + createIndexStatement.getTableName() + ", " +
                "index name = " + createIndexStatement.getIndexName() + ", " +
                "column name = " + column.getName() + "]";
    }
}
