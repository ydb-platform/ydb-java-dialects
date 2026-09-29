package tech.ydb.trino;

import io.trino.Session;
import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;
import io.trino.testing.BaseConnectorTest;
import io.trino.testing.MaterializedResult;
import io.trino.testing.QueryRunner;
import io.trino.testing.TestingConnectorBehavior;
import io.trino.testing.sql.JdbcSqlExecutor;
import io.trino.testing.sql.TestTable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static io.trino.sql.planner.assertions.PlanMatchPattern.anyTree;
import static io.trino.sql.planner.assertions.PlanMatchPattern.node;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestYdbConnectorTest extends BaseConnectorTest {

    @RegisterExtension
    static final YdbHelperExtension ydb = new YdbHelperExtension();

    @Override
    protected QueryRunner createQueryRunner() throws Exception {
        return YdbQueryRunner.builder(ydb)
                .setInitialTables(REQUIRED_TPCH_TABLES)
                .build();
    }

    @Test
    public void testDefaultSchema() {
        assertThat(computeActual("SHOW CATALOGS").getOnlyColumnAsSet()).contains("local");
        assertThat(computeActual("SHOW SCHEMAS FROM local").getOnlyColumnAsSet())
                .containsExactlyInAnyOrder("default", "information_schema");
        assertThat(computeActual("SHOW TABLES FROM local.default").getOnlyColumnAsSet()).contains("orders");
        assertQuerySucceeds("SELECT orderkey FROM local.default.orders LIMIT 1");
        assertQueryFails("SELECT * FROM local.missing.orders", ".*Schema 'missing' does not exist");
        assertQueryFails("SELECT * FROM local.\"%\".orders", ".*Schema '%' does not exist");
    }

    @Test
    public void testOptInInt64JoinPushdown() {
        Session session = Session.builder(getSession())
                .setCatalogSessionProperty(getSession().getCatalog().orElseThrow(), "join_pushdown_enabled", "true")
                .build();
        try (TestTable left = newTrinoTable("join_int64_left_", "(id bigint, k bigint, second_key bigint, d double, s varchar)",
                List.of("1, 7, 1, 1.0, 'a'", "2, 7, 2, 2.0, 'b'", "3, NULL, 1, 3.0, 'c'", "4, 8, 1, 4.0, 'd'"));
                TestTable right = newTrinoTable("join_int64_right_", "(id bigint, k bigint, second_key bigint, d double, s varchar)",
                        List.of("10, 7, 1, 1.0, 'a'", "11, 7, 2, 2.0, 'b'", "12, NULL, 1, 3.0, 'c'", "13, 9, 1, 4.0, 'd'"))) {
            String join = "SELECT l.id, r.id FROM " + left.getName() + " l %s " + right.getName() + " r ON %s";
            String matches = "VALUES (BIGINT '1', BIGINT '10'), (BIGINT '1', BIGINT '11'), " +
                    "(BIGINT '2', BIGINT '10'), (BIGINT '2', BIGINT '11')";
            assertThat(query(session, join.formatted("JOIN", "l.k = r.k"))).isFullyPushedDown().matches(matches);
            assertThat(query(session, join.formatted("LEFT JOIN", "l.k = r.k"))).isFullyPushedDown()
                    .matches(matches + ", (BIGINT '3', CAST(NULL AS BIGINT)), (BIGINT '4', CAST(NULL AS BIGINT))");
            assertThat(query(session, join.formatted("RIGHT JOIN", "l.k = r.k"))).isFullyPushedDown()
                    .matches(matches + ", (CAST(NULL AS BIGINT), BIGINT '12'), (CAST(NULL AS BIGINT), BIGINT '13')");
            assertThat(query(session, join.formatted("FULL JOIN", "l.k = r.k"))).isFullyPushedDown()
                    .matches(matches + ", (BIGINT '3', CAST(NULL AS BIGINT)), (BIGINT '4', CAST(NULL AS BIGINT)), " +
                            "(CAST(NULL AS BIGINT), BIGINT '12'), (CAST(NULL AS BIGINT), BIGINT '13')");

            assertThat(query(session, join.formatted("JOIN", "l.k = r.k AND l.second_key = r.second_key")))
                    .isFullyPushedDown()
                    .matches("VALUES (BIGINT '1', BIGINT '10'), (BIGINT '2', BIGINT '11')");
            assertThat(query(session, "SELECT l.id, r.id FROM (SELECT id, k FROM " + left.getName() +
                    " WHERE id > 1 AND id < 3) l JOIN (SELECT id, k FROM " + right.getName() +
                    " WHERE id > 10 AND id < 12) r ON l.k = r.k"))
                    .isFullyPushedDown()
                    .matches("VALUES (BIGINT '2', BIGINT '11')");

            assertThat(query(getSession(), join.formatted("JOIN", "l.k = r.k"))).joinIsNotFullyPushedDown();
            assertThat(query(session, join.formatted("JOIN", "l.k < r.k")))
                    .joinIsNotFullyPushedDown();
            assertThat(query(session, join.formatted("JOIN", "l.s = r.s")))
                    .matches("VALUES (BIGINT '1', BIGINT '10'), (BIGINT '2', BIGINT '11'), " +
                            "(BIGINT '3', BIGINT '12'), (BIGINT '4', BIGINT '13')")
                    .isFullyPushedDown();
            assertThat(query(session, join.formatted("JOIN", "l.d = r.d")))
                    .matches("VALUES (BIGINT '1', BIGINT '10'), (BIGINT '2', BIGINT '11'), " +
                            "(BIGINT '3', BIGINT '12'), (BIGINT '4', BIGINT '13')")
                    .isFullyPushedDown();
            Session complex = Session.builder(session)
                    .setCatalogSessionProperty(getSession().getCatalog().orElseThrow(), "complex_join_pushdown_enabled", "true")
                    .build();
            assertThat(query(complex, join.formatted("JOIN", "l.k = r.k"))).joinIsNotFullyPushedDown();
        }
    }

    @Test
    public void testScalarJoinKeys() {
        Session session = joinPushdownSession();
        try (TestTable table = newTrinoTable("join_scalar_",
                "(id bigint, bool_key boolean, tiny_key tinyint, small_key smallint, int_key integer, " +
                        "real_key real, double_key double, decimal_key decimal(10, 2), text_key varchar, " +
                        "binary_key varbinary, date_key date, timestamp_key timestamp(6))",
                List.of(
                        "1, true, -128, -32768, -2147483648, 1.5, 1.5, 1.50, 'Aλ', X'0080', DATE '2026-01-01', TIMESTAMP '2026-01-01 01:02:03.123456'",
                        "2, false, 127, 32767, 2147483647, 2.5, 2.5, -2.50, 'aλ ', X'FF', DATE '2026-01-02', TIMESTAMP '2026-01-01 01:02:03.123457'"))) {
            assertUpdate("INSERT INTO " + table.getName() + " (id) VALUES (3)", 1);
            for (String key : List.of("bool_key", "tiny_key", "small_key", "int_key", "real_key", "double_key",
                    "decimal_key", "text_key", "binary_key", "date_key", "timestamp_key")) {
                for (String join : List.of("JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN")) {
                    String sql = "SELECT l.id, r.id FROM " + table.getName() + " l " + join + " " + table.getName() +
                            " r ON l." + key + " = r." + key;
                    String expected = "VALUES (BIGINT '1', BIGINT '1'), (BIGINT '2', BIGINT '2')";
                    if (join.equals("LEFT JOIN") || join.equals("FULL JOIN")) {
                        expected += ", (BIGINT '3', CAST(NULL AS BIGINT))";
                    }
                    if (join.equals("RIGHT JOIN") || join.equals("FULL JOIN")) {
                        expected += ", (CAST(NULL AS BIGINT), BIGINT '3')";
                    }
                    assertThat(query(session, sql)).matches(expected).isFullyPushedDown();
                }
            }
        }
    }

    @Test
    public void testFloatingJoinKeys() {
        Session session = joinPushdownSession();
        try (TestTable table = newTrinoTable("join_floating_", "(id bigint, real_key real, double_key double)",
                List.of("1, REAL '0.0', DOUBLE '0.0'", "2, REAL '-0.0', DOUBLE '-0.0'",
                        "3, REAL 'NaN', DOUBLE 'NaN'", "4, REAL 'Infinity', DOUBLE 'Infinity'",
                        "5, REAL '-Infinity', DOUBLE '-Infinity'", "6, NULL, NULL"))) {
            String matches = "VALUES (BIGINT '1', BIGINT '1'), (BIGINT '1', BIGINT '2'), " +
                    "(BIGINT '2', BIGINT '1'), (BIGINT '2', BIGINT '2'), " +
                    "(BIGINT '4', BIGINT '4'), (BIGINT '5', BIGINT '5')";
            for (String key : List.of("real_key", "double_key")) {
                for (String join : List.of("JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN")) {
                    String sql = "SELECT l.id, r.id FROM " + table.getName() + " l " + join + " " + table.getName() +
                            " r ON l." + key + " = r." + key;
                    String expected = matches;
                    if (join.equals("LEFT JOIN") || join.equals("FULL JOIN")) {
                        expected += ", (BIGINT '3', CAST(NULL AS BIGINT)), (BIGINT '6', CAST(NULL AS BIGINT))";
                    }
                    if (join.equals("RIGHT JOIN") || join.equals("FULL JOIN")) {
                        expected += ", (CAST(NULL AS BIGINT), BIGINT '3'), (CAST(NULL AS BIGINT), BIGINT '6')";
                    }
                    assertThat(query(session, sql)).matches(expected).isFullyPushedDown();
                }
            }
        }
    }

    @Test
    public void testUnsignedJoinKeys() {
        Session session = joinPushdownSession();
        JdbcSqlExecutor remote = new JdbcSqlExecutor(YdbQueryRunner.buildJdbcUrl(ydb));
        try (TestTable table = new TestTable(remote, "join_unsigned_",
                "(id Int64 NOT NULL, u8 Uint8, u16 Uint16, u32 Uint32, u64 Uint64, signed_key Int64, PRIMARY KEY(id))")) {
            remote.execute("UPSERT INTO " + table.getName() + " (id, u8, u16, u32, u64, signed_key) VALUES " +
                    "(1, 255, 65535, 4294967295ul, 18446744073709551615ul, -1), (2, 0, 0, 0, 1, 1)");
            for (String key : List.of("u8", "u16", "u32", "u64")) {
                for (String join : List.of("JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN")) {
                    assertThat(query(session, "SELECT l.id, r.id FROM " + table.getName() + " l " + join +
                            " " + table.getName() + " r ON l." + key + " = r." + key))
                            .matches("VALUES (BIGINT '1', BIGINT '1'), (BIGINT '2', BIGINT '2')")
                            .isFullyPushedDown();
                }
            }
            assertThat(query(session, "SELECT l.id, r.id FROM " + table.getName() + " l JOIN " + table.getName() +
                    " r ON l.u64 = r.signed_key"))
                    .matches("VALUES (BIGINT '1', BIGINT '1'), (BIGINT '2', BIGINT '2')")
                    .isFullyPushedDown();
        }
    }

    @Test
    public void testComputedJoinKeys() {
        Session session = joinPushdownSession();
        Session local = Session.builder(getSession())
                .setSystemProperty("allow_pushdown_into_connectors", "false")
                .build();
        try (TestTable table = newTrinoTable("join_computed_",
                "(id bigint, k bigint, s smallint, i integer, text_key varchar, binary_key varbinary)",
                List.of("1, -2, -2, -2, 'A', X'00'", "2, -1, -1, -1, 'B', X'80'",
                        "3, 0, 0, 0, 'C', X'FF'", "4, 1, 1, 1, 'D', X'01'",
                        "5, 2, 2, 2, 'E', X'02'", "6, NULL, NULL, NULL, NULL, NULL"))) {
            for (String condition : List.of(
                    "l.k + 1 = r.k", "l.k - 1 = r.k", "l.k * 2 = r.k", "-l.k = r.k",
                    "CAST(l.s AS integer) = r.i", "CAST(l.s AS bigint) = r.k",
                    "CAST(l.i AS bigint) = CAST(r.i AS bigint)",
                    "l.text_key || 'suffix' = r.text_key || 'suffix'",
                    "l.binary_key || X'00' = r.binary_key || X'00'")) {
                for (String join : List.of("JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN")) {
                    String sql = "SELECT l.id, r.id FROM " + table.getName() + " l " + join + " " + table.getName() +
                            " r ON " + condition;
                    assertThat(query(session, sql)).matches(local, sql).isFullyPushedDown();
                }
            }
            assertThat(query(session, "SELECT l.id, r.id FROM " + table.getName() + " l LEFT JOIN " +
                    table.getName() + " r ON CAST(l.k AS smallint) = CAST(r.k AS smallint)"))
                    .joinIsNotFullyPushedDown();
        }
    }

    @Test
    public void testComputedJoinOverflow() {
        Session session = joinPushdownSession();
        Session local = Session.builder(getSession())
                .setSystemProperty("allow_pushdown_into_connectors", "false")
                .build();
        for (Map.Entry<String, String> test : Map.of(
                "%s + 1", "1, 9223372036854775807, -9223372036854775808",
                "%s - 1", "1, -9223372036854775808, 9223372036854775807",
                "-%s", "1, -9223372036854775808, -9223372036854775808",
                "%1$s * %1$s", "1, 9223372036854775807, 1").entrySet()) {
            try (TestTable table = newTrinoTable("join_overflow_", "(id bigint, k bigint, wrapped bigint)",
                    List.of(test.getValue()))) {
                String sql = "SELECT l.id, r.id FROM " + table.getName() + " l JOIN " + table.getName() +
                        " r ON " + test.getKey().formatted("l.k") + " = r.wrapped";
                assertThat(query(local, sql)).failure().hasMessageMatching("(?is).*overflow.*");
                assertThat(query(session, sql)).failure().hasMessageMatching("(?is).*overflow.*");
            }
        }
    }

    private Session joinPushdownSession() {
        return Session.builder(getSession())
                .setCatalogSessionProperty(getSession().getCatalog().orElseThrow(), "join_pushdown_enabled", "true")
                .build();
    }

    @Override
    protected void verifyConcurrentAddColumnFailurePermissible(Exception exception) {
        // YDB serializes overlapping scheme operations on one table and reports the conflict as retryable OVERLOADED:
        // https://ydb.tech/docs/en/reference/ydb-sdk/ydb-status-codes
        assertThat(exception)
                .hasMessageContaining("Status{code = OVERLOADED(code=400060)")
                .hasMessageContaining("error: path is under operation")
                .hasMessageContaining("state: EPathStateAlter");
    }

    @Override
    protected boolean hasBehavior(TestingConnectorBehavior connectorBehavior) {
        return switch (connectorBehavior) {
            case SUPPORTS_CREATE_VIEW,
                 SUPPORTS_CREATE_SCHEMA,
                 SUPPORTS_RENAME_SCHEMA,
                 SUPPORTS_SET_COLUMN_TYPE,
                 SUPPORTS_ROW_TYPE,
                 SUPPORTS_RENAME_COLUMN,
                 SUPPORTS_TRUNCATE,
                 SUPPORTS_COMMENT_ON_COLUMN,
                 SUPPORTS_COMMENT_ON_TABLE,
                 SUPPORTS_DROP_SCHEMA_CASCADE,
                 SUPPORTS_CREATE_MATERIALIZED_VIEW,
                 SUPPORTS_CREATE_TABLE_WITH_COLUMN_COMMENT,
                 SUPPORTS_CREATE_TABLE_WITH_TABLE_COMMENT,
                 SUPPORTS_ADD_COLUMN_WITH_COMMENT,
                 SUPPORTS_ADD_COLUMN_WITH_POSITION,
                 SUPPORTS_CREATE_FEDERATED_MATERIALIZED_VIEW,
                 SUPPORTS_RENAME_TABLE_ACROSS_SCHEMAS,
                 SUPPORTS_ARRAY,
                 SUPPORTS_MAP_TYPE,
                 SUPPORTS_DEFAULT_COLUMN_VALUE,
                 SUPPORTS_SET_DEFAULT_COLUMN_VALUE,
                 SUPPORTS_DROP_DEFAULT_COLUMN_VALUE,
                 SUPPORTS_ADD_COLUMN_NOT_NULL_CONSTRAINT -> false;
            case SUPPORTS_TOPN_PUSHDOWN_WITH_VARCHAR -> true;
            default -> super.hasBehavior(connectorBehavior);
        };
    }

    @Test
    @Override
    public void testCharVarcharComparison() {
        // YDB has no fixed-width string primitive. Mapping CHAR to Text loses its width in JDBC metadata
        // and violates Trino padding/coercion semantics: https://ydb.tech/docs/en/yql/reference/types/primitive
        assertThatThrownBy(super::testCharVarcharComparison)
                .hasMessage("Unsupported column type: char(3)");
    }

    @Override
    protected TestTable createTableWithDefaultColumns() {
        return new TestTable(
                new JdbcSqlExecutor(YdbQueryRunner.buildJdbcUrl(ydb)),
                "test_insert_default_",
                "(col_required Int64 NOT NULL, " +
                        "col_nullable Int64, " +
                        "col_default Int64 DEFAULT 43, " +
                        "col_nonnull_default Int64 NOT NULL DEFAULT 42, " +
                        "col_required2 Int64 NOT NULL, " +
                        "PRIMARY KEY (col_required))");
    }

    @Override
    protected String errorMessageForInsertIntoNotNullColumn(String columnName) {
        return "(?s).*("
                + "NULL value not allowed for NOT NULL column: " + columnName
                + "|Cannot set NULL to not nullable column: " + columnName
                + "|Missing value for not null column: " + columnName
                + "|Missing not null column in input: " + columnName
                + ").*";
    }

    @Override
    protected boolean isColumnNameRejected(Exception exception, String columnName, boolean delimited) {
        return requiresDelimiting(columnName);
    }

    @Override
    protected Optional<DataMappingTestSetup> filterDataMappingSmokeTestData(BaseConnectorTest.DataMappingTestSetup dataMappingTestSetup) {
        String trinoTypeName = dataMappingTestSetup.getTrinoTypeName();
        if (trinoTypeName.equals("char(3)")) {
            return Optional.of(dataMappingTestSetup.asUnsupported());
        } else if (trinoTypeName.equals("time")
                || trinoTypeName.equals("time(6)")
                || trinoTypeName.equals("timestamp(3) with time zone")
                || trinoTypeName.equals("timestamp(6) with time zone")) {
            // Нет time в YQL
            return Optional.empty();
        }
        return Optional.of(dataMappingTestSetup);
    }

    @Test
    public void testVarbinaryCreateTableAndInsert() {
        try (TestTable table = newTrinoTable("varbinary_insert_", "(id bigint, value varbinary)")) {
            assertUpdate("INSERT INTO " + table.getName() + " VALUES (1, X'0080FF')", 1);
            assertQuery("SELECT value FROM " + table.getName(), "VALUES X'0080FF'");
        }
    }

    @Override
    protected Optional<DataMappingTestSetup> filterCaseSensitiveDataMappingTestData(DataMappingTestSetup dataMappingTestSetup) {
        if (dataMappingTestSetup.getTrinoTypeName().equals("char(1)")) {
            return Optional.of(dataMappingTestSetup.asUnsupported());
        }
        return Optional.of(dataMappingTestSetup);
    }

    @Override
    protected MaterializedResult getDescribeOrdersResult() {
        // В YQL строки произвольной длины
        return MaterializedResult.resultBuilder(this.getSession(), new Type[]{VarcharType.VARCHAR, VarcharType.VARCHAR, VarcharType.VARCHAR, VarcharType.VARCHAR}).row(new Object[]{"orderkey", "bigint", "", ""}).row(new Object[]{"custkey", "bigint", "", ""}).row(new Object[]{"orderstatus", "varchar", "", ""}).row(new Object[]{"totalprice", "double", "", ""}).row(new Object[]{"orderdate", "date", "", ""}).row(new Object[]{"orderpriority", "varchar", "", ""}).row(new Object[]{"clerk", "varchar", "", ""}).row(new Object[]{"shippriority", "integer", "", ""}).row(new Object[]{"comment", "varchar", "", ""}).build();
    }

    @Test
    @Override
    public void testShowCreateTable() {
        // В YQL строки произвольной длины
        String catalog = this.getSession().getCatalog().orElseThrow();
        String schema = this.getSession().getSchema().orElseThrow();
        Assertions.assertThat(this.computeScalar("SHOW CREATE TABLE orders")).isEqualTo(String.format("CREATE TABLE %s.%s.orders (\n   orderkey bigint,\n   custkey bigint,\n   orderstatus varchar,\n   totalprice double,\n   orderdate date,\n   orderpriority varchar,\n   clerk varchar,\n   shippriority integer,\n   comment varchar\n)", catalog, schema));
    }

    @Override
    protected OptionalInt maxTableNameLength() {
        return OptionalInt.of(255);
    }

    @Override
    protected OptionalInt maxColumnNameLength() {
        return OptionalInt.of(255);
    }

    @Override
    protected void verifyTableNameLengthFailurePermissible(Throwable e) {
        assertThat(e.getMessage()).contains("too long");
    }

    @Override
    protected void verifyColumnNameLengthFailurePermissible(Throwable e) {
        assertThat(e.getMessage()).contains("too long");
    }

}
