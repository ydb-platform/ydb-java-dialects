package tech.ydb.trino;

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
import tech.ydb.scheme.SchemeClient;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.regex.Pattern;

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
    public void testDefaultSchemaContract() {
        assertThat(computeActual("SHOW SCHEMAS FROM ydb").getOnlyColumnAsSet())
                .contains("default")
                .doesNotContain("ydb");
        assertThat(computeActual("SHOW TABLES FROM ydb.default").getOnlyColumnAsSet())
                .contains("orders");
        assertQueryFails("SELECT * FROM ydb.missing.orders", ".*Schema 'missing' does not exist");
    }

    @Test
    public void testNestedTableMetadata() throws Exception {
        String directory = "namespace_" + uniqueSuffix();
        String table = directory + "/orders";
        createDirectory(directory);
        try (AutoCloseable ignoredDirectory = () -> removeDirectory(directory);
                AutoCloseable ignoredTable = () -> assertQuerySucceeds("DROP TABLE IF EXISTS \"" + table + "\"")) {
            createRawMetadataTable(table);
            assertThat(computeActual("SHOW TABLES").getOnlyColumnAsSet()).contains(table);
            assertQueryReturnsEmptyResult("SELECT id FROM \"" + table + "\"");
            assertThat(computeScalar("SHOW CREATE TABLE \"" + table + "\"").toString()).contains(table);
            assertQuery("SELECT column_name, data_type, is_nullable FROM information_schema.columns " +
                    "WHERE table_schema = 'default' AND table_name = '" + table + "' ORDER BY ordinal_position",
                    "VALUES ('id', 'bigint', 'NO'), ('note', 'varchar', 'YES')");
            assertThat(query("SELECT schema_name, table_name FROM system.metadata.table_comments " +
                    "WHERE catalog_name = 'ydb' AND schema_name = 'default'"))
                    .skippingTypesCheck().containsAll("VALUES ('default', '" + table + "')");
        }
    }

    @Test
    public void testInvalidTablePaths() throws Exception {
        String source = "test_invalid_paths_" + uniqueSuffix();
        assertUpdate("CREATE TABLE " + source + " (id bigint NOT NULL)");
        try (AutoCloseable ignoredSource = () -> assertQuerySucceeds("DROP TABLE IF EXISTS " + source)) {
            for (String table : List.of("/orders", "a//orders", "a/./orders", "a/../orders")) {
                String quotedTable = Pattern.quote(table);
                assertQueryFails("SELECT * FROM ydb.default.\"" + table + "\"", ".*Table 'ydb.default.\"" + quotedTable + "\"' does not exist");
                assertQuerySucceeds("DROP TABLE IF EXISTS ydb.default.\"" + table + "\"");
                assertQueryFails("CREATE TABLE ydb.default.\"" + table + "\" AS SELECT CAST(1 AS BIGINT) id", ".*Invalid YDB table path.*");
                assertQueryFails("CREATE TABLE ydb.default.\"" + table + "\" (id bigint NOT NULL)", ".*Invalid YDB table path '" + quotedTable + "'.*");
                assertQueryFails("ALTER TABLE " + source + " RENAME TO ydb.default.\"" + table + "\"", ".*Invalid YDB table path '" + quotedTable + "'.*");
            }
            assertThat(computeActual("SHOW TABLES").getOnlyColumnAsSet()).contains(source);
        }
    }

    @Test
    public void testCaseOnlyTablePathAmbiguity() throws Exception {
        String directory = "ambiguity_" + uniqueSuffix();
        String first = directory + "/Orders";
        String second = directory + "/orders";
        createDirectory(directory);
        try (AutoCloseable ignoredDirectory = () -> removeDirectory(directory);
                AutoCloseable ignoredFirst = () -> dropRawTable(first);
                AutoCloseable ignoredSecond = () -> dropRawTable(second)) {
            createRawTable(first);
            createRawTable(second);
            assertQueryFails("SELECT * FROM \"" + second + "\"", "(?s).*Ambiguous.*Orders.*orders.*");
            assertQueryFails("SELECT table_name FROM system.metadata.table_comments " +
                    "WHERE catalog_name = 'ydb' AND schema_name = 'default'", "(?s).*Ambiguous.*Orders.*orders.*");
        }
    }

    @Test
    public void testUniqueCaseTableWriteLifecycle() throws Exception {
        String suffix = uniqueSuffix();
        String remoteTable = "MixedCase_" + suffix;
        String logicalTable = remoteTable.toLowerCase(Locale.ROOT);
        String renamedTable = "renamed_" + suffix;

        try (AutoCloseable ignoredOriginalTable = () -> assertQuerySucceeds("DROP TABLE IF EXISTS \"" + logicalTable + "\"");
                AutoCloseable ignoredRenamedTable = () -> assertQuerySucceeds("DROP TABLE IF EXISTS \"" + renamedTable + "\"")) {
            createRawTable(remoteTable);

            assertUpdate("INSERT INTO \"" + logicalTable + "\" VALUES (1)", 1);
            assertQuerySucceeds("ALTER TABLE \"" + logicalTable + "\" RENAME TO \"" + renamedTable + "\"");

            assertQuery("SELECT id FROM \"" + renamedTable + "\"", "VALUES CAST(1 AS BIGINT)");
            assertThat(computeActual("SHOW TABLES").getOnlyColumnAsSet()).contains(renamedTable);
        }
    }

    @Test
    public void testMixedCaseDirectoryLifecycle() throws Exception {
        String directory = "Sales_" + uniqueSuffix();
        String logicalDirectory = directory.toLowerCase(Locale.ROOT);
        createDirectory(directory);
        try (AutoCloseable ignoredDirectory = () -> removeDirectory(directory);
                AutoCloseable ignoredTable = () -> assertQuerySucceeds("DROP TABLE IF EXISTS \"" + logicalDirectory + "/orders\"");
                AutoCloseable ignoredRenamed = () -> assertQuerySucceeds("DROP TABLE IF EXISTS \"" + logicalDirectory + "/renamed\"")) {
            assertUpdate("CREATE TABLE \"" + logicalDirectory + "/orders\" AS SELECT CAST(1 AS BIGINT) id", 1);
            assertUpdate("INSERT INTO \"" + logicalDirectory + "/orders\" VALUES 2", 1);
            assertQuerySucceeds("ALTER TABLE \"" + logicalDirectory + "/orders\" RENAME TO \"" + logicalDirectory + "/renamed\"");
            assertQuery("SELECT id FROM \"" + logicalDirectory + "/renamed\"", "VALUES CAST(1 AS BIGINT), CAST(2 AS BIGINT)");
            assertUpdate("CREATE TABLE \"" + logicalDirectory + "/orders\" (id bigint NOT NULL)");
            try (Connection connection = DriverManager.getConnection(YdbQueryRunner.buildJdbcUrl(ydb));
                    ResultSet tables = connection.getMetaData().getTables(null, null, directory + "/orders", null)) {
                assertThat(tables.next()).isTrue();
                assertThat(tables.getString("TABLE_NAME")).isEqualTo(directory + "/orders");
            }
        }
    }

    @Test
    public void testDirectoryDestinationConflicts() throws Exception {
        String directory = "Conflict_" + uniqueSuffix();
        String logical = directory.toLowerCase(Locale.ROOT);
        String source = "source_" + uniqueSuffix();
        createDirectory(directory);
        try (AutoCloseable ignoredDirectory = () -> removeDirectory(directory);
                AutoCloseable ignoredSource = () -> assertQuerySucceeds("DROP TABLE IF EXISTS " + source)) {
            assertUpdate("CREATE TABLE " + source + " AS SELECT CAST(7 AS BIGINT) id", 1);
            assertQueryFails("CREATE TABLE \"" + logical + "\" (id bigint)", ".*YDB object already exists.*");
            assertQueryFails("ALTER TABLE " + source + " RENAME TO \"" + logical + "\"", ".*YDB object already exists.*");
            assertQueryFails("CREATE TABLE \"" + source + "/child\" (id bigint)", ".*YDB parent directory does not exist.*");
            assertQueryFails("CREATE TABLE \"missing_" + uniqueSuffix() + "/child\" (id bigint)", ".*YDB parent directory does not exist.*");
            assertQuery("SELECT id FROM " + source, "VALUES CAST(7 AS BIGINT)");
        }
    }

    @Test
    public void testCaseOnlyParentAmbiguity() throws Exception {
        String directory = "Parent_" + uniqueSuffix();
        String other = directory.toLowerCase(Locale.ROOT);
        createDirectory(directory);
        try (AutoCloseable ignoredDirectory = () -> removeDirectory(directory)) {
            createDirectory(other);
            try (AutoCloseable ignoredOther = () -> removeDirectory(other)) {
                assertQueryFails("CREATE TABLE \"" + other + "/orders\" (id bigint)", ".*Ambiguous YDB path component.*");
                assertQueryFails("CREATE TABLE \"" + other + "/orders\" AS SELECT 1 id", ".*Ambiguous YDB path component.*");
            }
        }
    }

    @Test
    public void testDotPrefixedTable() throws Exception {
        String table = ".hidden_" + uniqueSuffix();
        createRawTable(table);
        try (AutoCloseable ignoredTable = () -> dropRawTable(table)) {
            assertThat(computeActual("SHOW TABLES").getOnlyColumnAsSet()).contains(table);
            assertQueryReturnsEmptyResult("SELECT id FROM \"" + table + "\"");
        }
    }

    @Test
    public void testEscapedTablePath() throws Exception {
        String table = "escaped_" + uniqueSuffix();
        String escaped = (ydb.database() + "/" + table).replace("/", "\\x2f");
        try (AutoCloseable ignoredLiteral = () -> assertQuerySucceeds("DROP TABLE IF EXISTS \"" + escaped + "\"");
                AutoCloseable ignoredDecoded = () -> assertQuerySucceeds("DROP TABLE IF EXISTS " + table)) {
            // A literal backslash escape must not become an absolute path in YQL.
            assertQuerySucceeds("CREATE TABLE \"" + escaped + "\" (id bigint)");
            assertThat(computeActual("SHOW TABLES").getOnlyColumnAsSet()).contains(escaped).doesNotContain(table);
        }
    }

    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static void createDirectory(String relativePath) {
        try (SchemeClient client = SchemeClient.newClient(ydb.createTransport()).build()) {
            client.makeDirectories(ydb.database() + "/" + relativePath).join().expectSuccess();
        }
    }

    private static void removeDirectory(String relativePath) {
        try (SchemeClient client = SchemeClient.newClient(ydb.createTransport()).build()) {
            client.removeDirectory(ydb.database() + "/" + relativePath).join().expectSuccess();
        }
    }

    private static void createRawTable(String remoteTable) throws SQLException {
        try (Connection connection = DriverManager.getConnection(YdbQueryRunner.buildJdbcUrl(ydb));
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE `" + remoteTable + "` (id Int64 NOT NULL, PRIMARY KEY (id))");
        }
    }

    private static void createRawMetadataTable(String remoteTable) throws SQLException {
        try (Connection connection = DriverManager.getConnection(YdbQueryRunner.buildJdbcUrl(ydb));
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE `" + remoteTable + "` " +
                    "(id Int64 NOT NULL, note Utf8, PRIMARY KEY (id))");
        }
    }

    private static void dropRawTable(String remoteTable) throws SQLException {
        try (Connection connection = DriverManager.getConnection(YdbQueryRunner.buildJdbcUrl(ydb));
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE `" + remoteTable + "`");
        }
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
