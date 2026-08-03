package tech.ydb.liquibase.sqlgenerator;

import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.core.TagDatabaseStatement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
public class TagDatabaseGeneratorYdbTest {

    @Test
    void generateSqlTest() {
        Sql[] sql = SqlGeneratorFactory.getInstance().generateSql(
                new TagDatabaseStatement("test"),
                new YdbDatabase()
        );

        assertEquals(1, sql.length);
        assertEquals(
                "UPDATE DATABASECHANGELOG ON SELECT * FROM (" +
                        "SELECT ID, AUTHOR, FILENAME, Utf8('test') AS TAG " +
                        "FROM DATABASECHANGELOG " +
                        "ORDER BY DATEEXECUTED DESC, ORDEREXECUTED DESC LIMIT 1)",
                sql[0].toSql()
        );
    }
}
