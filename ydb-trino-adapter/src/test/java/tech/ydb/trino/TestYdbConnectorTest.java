package tech.ydb.trino;

import com.google.common.collect.ImmutableMap;
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
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalInt;

import static io.trino.testing.TestingNames.randomNameSuffix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestYdbConnectorTest extends BaseConnectorTest {
    private static final String ALTERNATE_DATE_CATALOG = "alternate_dates";
    private static final boolean FORCE_SIGNED_DATETIMES = Boolean.getBoolean("ydb.test.force-signed-datetimes");

    @RegisterExtension
    static final YdbHelperExtension ydb = new YdbHelperExtension();

    @Override
    protected QueryRunner createQueryRunner() throws Exception {
        String jdbcUrl = YdbQueryRunner.buildJdbcUrl(ydb);
        QueryRunner queryRunner = YdbQueryRunner.builder(ydb)
                .addConnectorProperty("connection-url", jdbcUrl + "&forceSignedDatetimes=" + FORCE_SIGNED_DATETIMES)
                .setInitialTables(REQUIRED_TPCH_TABLES)
                .build();
        queryRunner.createCatalog(ALTERNATE_DATE_CATALOG, "ydb", ImmutableMap.of(
                "connection-url", jdbcUrl + "&forceSignedDatetimes=" + !FORCE_SIGNED_DATETIMES,
                "insert.non-transactional-insert.enabled", "true"));
        return queryRunner;
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
                 SUPPORTS_ADD_COLUMN_NOT_NULL_CONSTRAINT -> false;
            case SUPPORTS_TOPN_PUSHDOWN_WITH_VARCHAR -> true;
            case SUPPORTS_NEGATIVE_DATE -> FORCE_SIGNED_DATETIMES;
            default -> super.hasBehavior(connectorBehavior);
        };
    }

    @Override
    protected String errorMessageForInsertNegativeDate(String date) {
        return ".*negative daysSinceEpoch.*";
    }

    @Override
    protected String errorMessageForCreateTableAsSelectNegativeDate(String date) {
        return ".*negative daysSinceEpoch.*";
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
        if (dataMappingTestSetup.getTrinoTypeName().equals("char(3)")) {
            return Optional.of(dataMappingTestSetup.asUnsupported());
        } else if (dataMappingTestSetup.getTrinoTypeName().equals("date") && !FORCE_SIGNED_DATETIMES) {
            return Optional.of(new DataMappingTestSetup(
                    dataMappingTestSetup.getTrinoTypeName(),
                    "DATE '2006-06-06'",
                    "DATE '2026-06-06'"
            ));
        } else if (dataMappingTestSetup.getTrinoTypeName().startsWith("time")) {
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

    @Test
    public void testNativeDateCompatibility() throws Exception {
        verifyNativeDateCompatibility("local");
        verifyNativeDateCompatibility(ALTERNATE_DATE_CATALOG);
    }

    private void verifyNativeDateCompatibility(String catalog) throws Exception {
        String ddlTable = "trino_date_" + randomNameSuffix();
        boolean signed = catalog.equals("local") ? FORCE_SIGNED_DATETIMES : !FORCE_SIGNED_DATETIMES;
        try (TestTable table = new TestTable(
                new JdbcSqlExecutor(YdbQueryRunner.buildJdbcUrl(ydb)),
                "native_dates_",
                "(legacy_key Date NOT NULL, signed_key Date32 NOT NULL, legacy_value Date, signed_value Date32, " +
                        "PRIMARY KEY (legacy_key, signed_key))")) {
            String name = catalog + ".default." + table.getName();
            try (Connection connection = DriverManager.getConnection(YdbQueryRunner.buildJdbcUrl(ydb));
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO `" + table.getName() + "` (legacy_key, signed_key) VALUES (?, ?)")) {
                statement.setObject(1, PrimitiveValue.newDate(LocalDate.of(2020, 1, 1)));
                statement.setObject(2, PrimitiveValue.newDate32(LocalDate.of(-1, 1, 1)));
                statement.executeUpdate();
            }
            assertQueryReturnsEmptyResult("SELECT * FROM " + name + " WHERE legacy_key = DATE '-0001-01-01'");
            assertQuery("SELECT count(*) FROM " + name + " WHERE legacy_key > DATE '-0001-01-01'", "VALUES BIGINT '1'");
            assertQueryReturnsEmptyResult("SELECT * FROM " + name + " WHERE legacy_key = DATE '2106-01-01'");
            assertQuery("SELECT count(*) FROM " + name + " WHERE legacy_key < DATE '2106-01-01'", "VALUES BIGINT '1'");
            try (Connection connection = DriverManager.getConnection(YdbQueryRunner.buildJdbcUrl(ydb));
                    PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM `" + table.getName() + "` WHERE legacy_key = ? AND signed_key = ?")) {
                statement.setObject(1, PrimitiveValue.newDate(LocalDate.of(2020, 1, 1)));
                statement.setObject(2, PrimitiveValue.newDate32(LocalDate.of(-1, 1, 1)));
                statement.executeUpdate();
            }
            assertUpdate("INSERT INTO " + name + " VALUES " +
                    "(DATE '2020-01-01', DATE '-0001-01-01', DATE '2000-01-01', NULL), " +
                    "(DATE '2020-01-02', DATE '-0001-01-02', NULL, DATE '-0002-01-01'), " +
                    "(DATE '2020-01-01', DATE '-0001-01-04', DATE '2004-01-01', NULL)", 3);
            assertQuery("SELECT legacy_value, signed_value FROM " + name +
                    " WHERE legacy_key = DATE '2020-01-01' AND signed_key = DATE '-0001-01-01'",
                    "VALUES (DATE '2000-01-01', CAST(NULL AS DATE))");
            assertUpdate("UPDATE " + name + " SET legacy_value = DATE '2001-01-01', signed_value = DATE '-0003-01-01'" +
                    " WHERE legacy_key = DATE '2020-01-01' AND signed_key = DATE '-0001-01-01'", 1);
            assertUpdate("""
                    MERGE INTO %s t
                    USING (VALUES
                        (DATE '2020-01-01', DATE '-0001-01-01', CAST(NULL AS DATE), DATE '-0004-01-01', 'update'),
                        (DATE '2020-01-02', DATE '-0001-01-02', CAST(NULL AS DATE), CAST(NULL AS DATE), 'delete'),
                        (DATE '2020-01-03', DATE '-0001-01-03', DATE '2003-01-01', CAST(NULL AS DATE), 'insert')
                    ) s (legacy_key, signed_key, legacy_value, signed_value, operation)
                    ON (t.legacy_key = s.legacy_key AND t.signed_key = s.signed_key)
                    WHEN MATCHED AND s.operation = 'delete' THEN DELETE
                    WHEN MATCHED THEN UPDATE SET legacy_value = s.legacy_value, signed_value = s.signed_value
                    WHEN NOT MATCHED THEN INSERT VALUES (s.legacy_key, s.signed_key, s.legacy_value, s.signed_value)
                    """.formatted(name), 3);
            assertQuery("SELECT legacy_key, signed_key, legacy_value, signed_value FROM " + name + " ORDER BY legacy_key",
                    "VALUES " +
                            "(DATE '2020-01-01', DATE '-0001-01-01', NULL, DATE '-0004-01-01'), " +
                            "(DATE '2020-01-01', DATE '-0001-01-04', DATE '2004-01-01', NULL), " +
                            "(DATE '2020-01-03', DATE '-0001-01-03', DATE '2003-01-01', NULL)");
            try (Connection connection = DriverManager.getConnection(YdbQueryRunner.buildJdbcUrl(ydb));
                    Statement statement = connection.createStatement();
                    ResultSet rows = statement.executeQuery("SELECT * FROM `" + table.getName() + "` ORDER BY legacy_key, signed_key")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getObject("legacy_value", LocalDate.class)).isNull();
                assertThat(rows.getObject("signed_value", LocalDate.class)).isEqualTo(LocalDate.of(-4, 1, 1));
            }
        }
        try {
            assertUpdate("CREATE TABLE " + catalog + ".default." + ddlTable + " (value DATE)");
            try (Connection connection = DriverManager.getConnection(YdbQueryRunner.buildJdbcUrl(ydb));
                    ResultSet columns = connection.getMetaData().getColumns(null, null, ddlTable, "value")) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getString("TYPE_NAME")).isEqualTo(signed ? "Date32" : "Date");
            }
        }
        finally {
            assertUpdate("DROP TABLE IF EXISTS " + catalog + ".default." + ddlTable);
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
