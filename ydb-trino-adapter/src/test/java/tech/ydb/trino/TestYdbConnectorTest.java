package tech.ydb.trino;

import io.trino.Session;
import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;
import io.trino.sql.planner.plan.ExchangeNode;
import io.trino.sql.planner.plan.JoinNode;
import io.trino.sql.planner.plan.TableScanNode;
import io.trino.sql.planner.plan.TopNNode;
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
                 SUPPORTS_ADD_COLUMN_NOT_NULL_CONSTRAINT,
                 SUPPORTS_JOIN_PUSHDOWN_WITH_DISTINCT_FROM,
                 SUPPORTS_JOIN_PUSHDOWN_WITH_VARCHAR_EQUALITY,
                 SUPPORTS_JOIN_PUSHDOWN_WITH_VARCHAR_INEQUALITY -> false;
            case SUPPORTS_TOPN_PUSHDOWN_WITH_VARCHAR,
                 SUPPORTS_JOIN_PUSHDOWN,
                 SUPPORTS_JOIN_PUSHDOWN_WITH_FULL_JOIN -> true;
            default -> super.hasBehavior(connectorBehavior);
        };
    }

    // JOIN contracts from Trino 483, kept here without enabling unrelated JDBC fixture tests:
    // https://github.com/trinodb/trino/blob/483/plugin/trino-base-jdbc/src/test/java/io/trino/plugin/jdbc/BaseJdbcConnectorTest.java
    @Test
    public void testJoinPushdownDisabled() {
        Session session = Session.builder(getSession())
                .setCatalogSessionProperty(getSession().getCatalog().orElseThrow(), "join_pushdown_enabled", "false")
                .setSystemProperty("enable_dynamic_filtering", "false")
                .build();
        assertThat(query(session, "SELECT r.name, n.name FROM nation n JOIN region r ON n.regionkey = r.regionkey"))
                .joinIsNotFullyPushedDown();
    }

    @Test
    public void testJoinPushdown() {
        // YQL requires equality between the two sources; one-sided outer JOIN conditions stay in Trino:
        // https://ydb.tech/docs/en/yql/reference/syntax/select/join
        // Trino 483 hard-codes the opposite expectation in static expectJoinPushdownOnEmptyProjection:
        // https://github.com/trinodb/trino/blob/483/plugin/trino-base-jdbc/src/test/java/io/trino/plugin/jdbc/BaseJdbcConnectorTest.java#L1391-L1394
        Session session = Session.builder(getSession())
                .setSystemProperty("enable_dynamic_filtering", "false")
                .build();
        try (TestTable lowercaseNation = newTrinoTable(
                "nation_lowercase", "AS SELECT nationkey, lower(name) name, regionkey FROM nation")) {
            for (String join : List.of("JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN")) {
                for (String sql : List.of(
                        "SELECT r.name, n.name FROM nation n %s region r ON n.regionkey = r.regionkey",
                        "SELECT r.name, n.name FROM nation n %s region r ON n.nationkey = r.regionkey",
                        "SELECT r.name, n.name FROM nation n %s region r USING (regionkey)",
                        "SELECT n.name, c.name FROM nation n %s customer c ON n.nationkey = c.nationkey AND n.regionkey = c.custkey",
                        "SELECT c.name, n.name FROM (SELECT * FROM customer WHERE acctbal > 8000) c %s nation n ON c.custkey = n.nationkey",
                        "SELECT c.name, n.name FROM (SELECT * FROM customer WHERE address = 'TcGe5gaZNgVePxU5kRrvXBfkasDTea') c %s nation n ON c.custkey = n.nationkey",
                        "SELECT c.name, n.name FROM (SELECT * FROM customer WHERE address < 'TcGe5gaZNgVePxU5kRrvXBfkasDTea') c %s nation n ON c.custkey = n.nationkey",
                        "SELECT * FROM (SELECT regionkey rk, count(nationkey) c FROM nation GROUP BY regionkey) n %s region r ON n.rk = r.regionkey",
                        "SELECT * FROM (SELECT nationkey FROM nation LIMIT 30) n %s region r ON n.nationkey = r.regionkey",
                        "SELECT * FROM (SELECT nationkey FROM nation ORDER BY regionkey LIMIT 5) n %s region r ON n.nationkey = r.regionkey",
                        "SELECT count(*) FROM nation n %s region r ON n.regionkey = r.regionkey")) {
                    assertThat(query(session, sql.formatted(join))).isFullyPushedDown();
                }

                for (String sql : List.of(
                        "SELECT n.name FROM nation n %s orders o ON DATE '2025-03-19' = o.orderdate",
                        "SELECT n.name FROM nation n %s region r ON n.regionkey = 1",
                        "SELECT n.name, r.name FROM nation n %s region r ON n.nationkey = n.regionkey",
                        "SELECT n.name, r.name FROM nation n %s region r ON n.regionkey + 1 = r.regionkey",
                        "SELECT n.name, n2.regionkey FROM nation n %s nation n2 ON n.name = n2.name",
                        "SELECT * FROM (SELECT regionkey, count(*) c FROM nation GROUP BY regionkey) n %s region r ON n.c = r.regionkey")) {
                    assertThat(query(session, sql.formatted(join))).joinIsNotFullyPushedDown();
                }

                for (String operator : List.of("=", "<>", "<", "<=", ">", ">=", "IS DISTINCT FROM", "IS NOT DISTINCT FROM")) {
                    assertThat(query(session, "SELECT n.name, nl.name FROM nation n " + join + " " + lowercaseNation.getName() +
                            " nl ON n.name " + operator + " nl.name"))
                            .joinIsNotFullyPushedDown();
                    assertThat(query(session, "SELECT n.name, nl.name FROM nation n " + join + " " + lowercaseNation.getName() +
                            " nl ON n.regionkey = nl.regionkey AND n.name " + operator + " nl.name"))
                            .joinIsNotFullyPushedDown();
                    if (!operator.equals("=")) {
                        assertThat(query(session, "SELECT r.name, n.name FROM nation n " + join +
                                " region r ON n.regionkey " + operator + " r.regionkey"))
                                .joinIsNotFullyPushedDown();
                        assertThat(query(session, "SELECT n.name, c.name FROM nation n " + join +
                                " customer c ON n.nationkey = c.nationkey AND n.regionkey " + operator + " c.custkey"))
                                .joinIsNotFullyPushedDown();
                    }
                }
            }
        }
        assertThat(query(session, "SELECT * FROM nation n, region r, customer c " +
                "WHERE n.regionkey = r.regionkey AND r.regionkey = c.custkey"))
                .isFullyPushedDown();
    }

    @Test
    public void testComplexJoinPushdown() {
        // Arithmetic predicates are supported individually, but this condition mixes both JOIN sources:
        // https://ydb.tech/docs/en/yql/reference/syntax/select/join
        for (boolean complex : List.of(false, true)) {
            Session session = Session.builder(getSession())
                    .setCatalogSessionProperty(getSession().getCatalog().orElseThrow(), "complex_join_pushdown_enabled", Boolean.toString(complex))
                    .build();
            assertThat(query(session, "SELECT n.name, o.orderstatus FROM nation n JOIN orders o " +
                    "ON n.regionkey = o.orderkey AND n.nationkey + o.custkey - 3 = 0"))
                    .joinIsNotFullyPushedDown();
            if (complex) {
                assertThat(query(session, "SELECT n.name, r.name FROM nation n JOIN region r ON n.regionkey = r.regionkey"))
                        .joinIsNotFullyPushedDown();
            }
        }
    }

    @Test
    public void testLimitPushdownWithDistinctAndJoin() {
        assertThat(query("SELECT DISTINCT regionkey FROM nation LIMIT 5")).isFullyPushedDown();
        assertThat(query(getSession(),
                "SELECT n.name, r.name FROM nation n LEFT JOIN region r USING (regionkey) LIMIT 30"))
                .isFullyPushedDown();
    }

    @Test
    public void testTopNPushdownWithJoin() {
        Session session = Session.builder(getSession())
                .setCatalogSessionProperty(getSession().getCatalog().orElseThrow(), "join_pushdown_enabled", "false")
                .build();
        assertThat(query(session, "SELECT * FROM nation n LEFT JOIN region r ON n.regionkey = r.regionkey " +
                "ORDER BY n.nationkey LIMIT 3"))
                .ordered()
                .isNotFullyPushedDown(node(TopNNode.class,
                        anyTree(node(JoinNode.class,
                                node(ExchangeNode.class, node(TableScanNode.class)),
                                anyTree(node(TableScanNode.class))))));
    }

    @Test
    public void testJoinPushdownWithLongIdentifiers() {
        String column = "col" + "z".repeat(maxColumnNameLength().orElseThrow() - 3);
        try (TestTable left = newTrinoTable("test_long_id_l", "(" + column + " BIGINT)", List.of("1"));
                TestTable right = newTrinoTable("test_long_id_r", "(" + column + " BIGINT)", List.of("1", "2"))) {
            assertThat(query(getSession(),
                    "SELECT l.%1$s, r.%1$s FROM %2$s l JOIN %3$s r ON l.%1$s = r.%1$s"
                            .formatted(column, left.getName(), right.getName())))
                    .isFullyPushedDown();
        }
    }

    @Test
    public void testYdbJoinNullsAndDuplicates() {
        Session session = getSession();
        try (TestTable left = newTrinoTable("join_null_l", "(id bigint, k bigint)",
                List.of("1, 10", "2, 10", "3, NULL", "4, -9223372036854775808", "5, 20"));
                TestTable right = newTrinoTable("join_null_r", "(id bigint, k bigint)",
                        List.of("11, 10", "12, 10", "13, NULL", "14, -9223372036854775808", "15, 30"))) {
            for (String join : List.of("JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN")) {
                assertThat(query(session, "SELECT l.id, r.id FROM " + left.getName() + " l " + join +
                        " " + right.getName() + " r ON r.k = l.k"))
                        .isFullyPushedDown();
            }
            assertThat(query(session, "SELECT count(*) FROM " + left.getName() + " l JOIN " + right.getName() + " r ON l.k = r.k"))
                    .matches("VALUES BIGINT '5'")
                    .isFullyPushedDown();
        }
    }

    @Test
    public void testYdbJoinParametersAndNestedJoin() {
        Session session = getSession();
        try (TestTable left = newTrinoTable("join_param_l", "(id bigint, k bigint)", List.of("11, 2", "12, 4"));
                TestTable right = newTrinoTable("join_param_r", "(id bigint, k bigint)", List.of("31, 2", "32, 12"))) {
            for (String predicate : List.of("id = 31", "id > 30")) {
                String joined = "SELECT l.id left_id, r.id right_id FROM " +
                        "(SELECT * FROM " + left.getName() + " WHERE id > 10 AND id < 12) l JOIN " +
                        "(SELECT * FROM " + right.getName() + " WHERE " + predicate + ") r ON l.k = r.k";
                assertThat(query(session, joined))
                        .matches("VALUES (BIGINT '11', BIGINT '31')")
                        .isFullyPushedDown();
                var nested = assertThat(query(session, "SELECT j.left_id, r.k FROM (" + joined + ") j JOIN " +
                        right.getName() + " r ON j.right_id = r.id"))
                        .matches("VALUES (BIGINT '11', BIGINT '2')");
                if (predicate.equals("id = 31")) {
                    // Constant propagation removes the outer equi-condition, leaving a CROSS JOIN in Trino.
                    nested.joinIsNotFullyPushedDown();
                } else {
                    nested.isFullyPushedDown();
                }
            }
        }
    }

    @Test
    public void testYdbJoinTypeAndExpressionFallback() {
        Session session = getSession();
        try (TestTable table = newTrinoTable("join_types",
                "(id bigint, small_key integer, float_key double, decimal_key decimal(10, 2), text_key varchar, date_key date)",
                List.of("1, 1, 1.5, 1.50, 'A', DATE '2026-01-01'", "2, 2, 2.5, 2.50, 'a ', DATE '2026-01-02'"))) {
            for (String condition : List.of(
                    "l.small_key = r.small_key", "l.float_key = r.float_key", "l.decimal_key = r.decimal_key",
                    "l.text_key = r.text_key", "l.date_key = r.date_key", "l.id = r.small_key",
                    "l.id + 1 = r.id", "l.id = r.id OR l.id = r.id + 1")) {
                assertThat(query(session, "SELECT l.id, r.id FROM " + table.getName() +
                        " l LEFT JOIN " + table.getName() + " r ON " + condition))
                        .joinIsNotFullyPushedDown();
            }
        }
    }

    @Test
    public void testYdbJoinAcrossCatalogs() {
        assertThat(query(getSession(),
                "SELECT count(*) FROM nation n JOIN tpch.tiny.region r ON n.regionkey = r.regionkey"))
                .matches("VALUES BIGINT '25'")
                .joinIsNotFullyPushedDown();
    }

    @Test
    @Override
    public void testInsertNegativeDate() {
        // YDB не поддерживает, negative daysSinceEpoch
    }

    @Test
    @Override
    public void testDateYearOfEraPredicate() {
        // YDB не поддерживает, negative daysSinceEpoch
    }

    @Test
    @Override
    public void testCreateTableAsSelectNegativeDate() {
        // YDB не поддерживает, negative daysSinceEpoch
    }

    @Test
    @Override
    public void testCharVarcharComparison() {
        // YDB has no fixed-width string primitive. Mapping CHAR to Text loses its width in JDBC metadata
        // and violates Trino padding/coercion semantics: https://ydb.tech/docs/en/yql/reference/types/primitive
        assertThatThrownBy(super::testCharVarcharComparison)
                .hasMessage("Unsupported column type: char(3)");
    }

    @Test
    @Override
    public void testVarcharCastToDateInPredicate() {
        // YDB не поддерживает такой pushdown/cast
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
        if (dataMappingTestSetup.getTrinoTypeName().equals("char(3)")) {
            return Optional.of(dataMappingTestSetup.asUnsupported());
        } else if (dataMappingTestSetup.getTrinoTypeName().equals("date")) {
            return Optional.of(new DataMappingTestSetup(
                    dataMappingTestSetup.getTrinoTypeName(),
                    "DATE '2006-06-06'",
                    "DATE '2026-06-06'"
            ));
        } else if (dataMappingTestSetup.getTrinoTypeName().startsWith("time") || dataMappingTestSetup.getTrinoTypeName().equals("varbinary")) {
            // Нет time и varbinary в YQL
            return Optional.empty();
        }
        return Optional.of(dataMappingTestSetup);
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
