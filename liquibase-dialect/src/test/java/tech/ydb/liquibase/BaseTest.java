package tech.ydb.liquibase;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.command.CommandScope;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CommandExecutionException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

/**
 * @author Kirill Kurdyukov
 */

public abstract class BaseTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    protected static String jdbcUrl() {
        StringBuilder jdbc = new StringBuilder("jdbc:ydb:")
                .append(ydb.useTls() ? "grpcs://" : "grpc://")
                .append(ydb.endpoint())
                .append(ydb.database());

        if (ydb.authToken() != null) {
            jdbc.append("?").append("token=").append(ydb.authToken());
        }

        return jdbc.toString();
    }

    @AfterEach
    public void clearMetaLiquibaseTables() {
        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            Statement statement = connection.createStatement();

            statement.execute("" +
                    "DROP TABLE DATABASECHANGELOGLOCK;" +
                    "DROP TABLE DATABASECHANGELOG;"
            );
        } catch (Exception e) {
            // do nothing
        }
    }

    protected static void migrateChangeFile(String changeLogFile) throws CommandExecutionException {
        assertEquals(0, new CommandScope("update")
                .addArgumentValue("changeLogFile", changeLogFile)
                .addArgumentValue("url", jdbcUrl())
                .execute().getResult("statusCode")
        );
    }

    protected static String migrationStr(String changeLogFile) throws SQLException, LiquibaseException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            Liquibase liquibase = new Liquibase(
                    changeLogFile,
                    new ClassLoaderResourceAccessor(),
                    new JdbcConnection(connection)
            );

            StringWriter stringWriter = new StringWriter();

            liquibase.update(new Contexts(), stringWriter);

            return stringWriter.toString();
        }
    }
}
