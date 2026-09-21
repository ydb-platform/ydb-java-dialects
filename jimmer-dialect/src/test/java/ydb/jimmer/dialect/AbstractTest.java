package ydb.jimmer.dialect;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.DefaultExecutor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import tech.ydb.test.junit5.YdbHelperExtension;
import ydb.jimmer.dialect.scalar.DurationProvider;
import ydb.jimmer.dialect.sqlMonitor.YdbExecutorMonitor;
import ydb.jimmer.dialect.transaction.YqlClient;
import ydb.jimmer.dialect.transaction.YdbTxConnectionManager;

import javax.sql.DataSource;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class AbstractTest {
    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    protected static final YdbExecutorMonitor executor = new YdbExecutorMonitor(
            new YdbExecutor(DefaultExecutor.INSTANCE)
    );
    private static final JSqlClient yqlClient;
    private static final JSqlClient yqlClientForBatch;

    static {
        yqlClient = JSqlClient.newBuilder()
                .setDialect(new YdbDialect())
                .setExecutor(executor)
                .addScalarProvider(new DurationProvider())
                .build();

        yqlClientForBatch = JSqlClient.newBuilder()
                .setDialect(new YdbDialect())
                .setExecutor(executor)
                .setExplicitBatchEnabled(true)
                .setDumbBatchAcceptable(true)
                .build();
    }

    protected static JSqlClient getYqlClient() {
        return yqlClient;
    }

    protected static JSqlClient getYqlClientForBatch() {
        return yqlClientForBatch;
    }

    protected static YqlClient getIsolationClient() {
        DataSource dataSource = new DriverManagerDataSource(getJdbcURL());
        return new YqlClient(
                (JSqlClientImplementor) JSqlClient.newBuilder()
                        .setConnectionManager(new YdbTxConnectionManager(dataSource))
                        .setDialect(new YdbDialect())
                        .setExecutor(executor)
                        .build());
    }

    protected void initDatabase() {
        try (Connection connection = DriverManager.getConnection(getJdbcURL())) {
            URL dropTablesUrl = AbstractTest.class.getClassLoader().getResource("database-drop-tables-ydb.sql");
            if (dropTablesUrl == null) {
                throw new IllegalStateException("Cannot load 'database-drop-tables-ydb.sql'");
            }
            try {
                executeYqlScript(connection, dropTablesUrl);
            } catch (ScriptException e) {
                //
            }

            URL url = AbstractTest.class.getClassLoader().getResource("database-ydb.sql");
            if (url == null) {
                throw new IllegalStateException("Cannot load 'database-ydb.sql'");
            }
            executeYqlScript(connection, url);
        } catch (SQLException e) {
            Assertions.fail("Database threw an exception: " + e.getMessage());
        }
    }

    private void executeYqlScript(Connection connection, URL url) throws ScriptException {
        ScriptUtils.executeSqlScript(
                connection,
                new EncodedResource(new UrlResource(url)),
                false,
                false,
                "--",
                ";",
                "/*",
                "*/");
    }

    protected static String getJdbcURL() {
        StringBuilder jdbc = new StringBuilder("jdbc:ydb:")
                .append(ydb.useTls() ? "grpcs://" : "grpc://")
                .append(ydb.endpoint())
                .append("/")
                .append(ydb.database());

        if (ydb.authToken() != null) {
            jdbc.append("?").append("token=").append(ydb.authToken());
        }

        return jdbc.toString();
    }

    protected static void createTable(String tableName, String typeName) {
        executeSql(
                "CREATE TABLE " + tableName + "(" +
                        "id Int8," +
                        "value " + typeName + "," +
                        "PRIMARY KEY (id)" +
                        ")");
    }

    protected static void dropTable(String tableName) {
        executeSql("DROP TABLE " + tableName);
    }

    protected static void executeSql(String sql) {
        try (Connection connection = DriverManager.getConnection(getJdbcURL())) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            Assertions.fail("Database threw an exception: " + e.getMessage());
        }
    }
}
