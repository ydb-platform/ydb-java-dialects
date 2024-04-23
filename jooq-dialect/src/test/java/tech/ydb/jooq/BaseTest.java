package tech.ydb.jooq;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

public abstract class BaseTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    protected static DSLContext dsl;

    @BeforeAll
    public static void beforeAll() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl());
        dsl = new YdbDslContext(conn);

        conn.createStatement()
                .execute("CREATE TABLE series \n" +
                        "(                           \n" +
                        "    series_id Uint64,\n" +
                        "    title Utf8,\n" +
                        "    series_info Utf8,\n" +
                        "    release_date Uint64,\n" +
                        "    PRIMARY KEY (series_id)\n" +
                        ");\n" +
                        "\n" +
                        "CREATE TABLE seasons\n" +
                        "(\n" +
                        "    series_id Uint64,\n" +
                        "    season_id Uint64,\n" +
                        "    title Utf8,\n" +
                        "    first_aired Uint64,\n" +
                        "    last_aired Uint64,\n" +
                        "    PRIMARY KEY (series_id, season_id)\n" +
                        ");\n" +
                        "\n" +
                        "CREATE TABLE episodes\n" +
                        "(\n" +
                        "    series_id Uint64,\n" +
                        "    season_id Uint64,\n" +
                        "    episode_id Uint64,\n" +
                        "    title Utf8,\n" +
                        "    air_date Uint64,\n" +
                        "    PRIMARY KEY (series_id, season_id, episode_id)\n" +
                        ");" +
                        "" +
                        "CREATE TABLE hard_table\n" +
                        "(\n" +
                        "    id text,\n" +
                        "    first json,\n" +
                        "    second jsondocument,\n" +
                        "    third yson,\n" +
                        "    PRIMARY KEY (id)\n" +
                        ");");
    }

    @AfterEach
    public void afterEach() {
        List<Table<?>> tables = dsl.meta().getTables();

        for (Table<?> table : tables) {
            if (!table.getName().startsWith(".sys")) {
                dsl.deleteFrom(table).execute();
            }
        }
    }

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
}
