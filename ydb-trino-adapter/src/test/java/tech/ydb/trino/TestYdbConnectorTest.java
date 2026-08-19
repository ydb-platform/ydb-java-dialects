package tech.ydb.trino;

import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;
import io.trino.testing.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.scheme.SchemeClient;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
    public void testNestedTablePath() {
        String namespace = "namespace_" + uniqueSuffix();
        String directory = namespace + "/eu";
        String table = directory + "/orders";

        createDirectory(directory);
        try {
            assertQuerySucceeds("CREATE TABLE \"" + table + "\" (id bigint NOT NULL)");
            assertThat(computeActual("SHOW TABLES").getOnlyColumnAsSet()).contains(table);
            assertQueryReturnsEmptyResult("SELECT id FROM \"" + table + "\"");
            assertThat(computeScalar("SHOW CREATE TABLE \"" + table + "\"").toString()).contains(table);
        }
        finally {
            assertQuerySucceeds("DROP TABLE IF EXISTS \"" + table + "\"");
            removeDirectory(directory);
            removeDirectory(namespace);
        }
    }

    @Test
    public void testInvalidTablePaths() {
        for (String table : List.of("/orders", "a//orders", "a/../orders", ".sys/orders")) {
            assertQueryFails("SELECT * FROM ydb.default.\"" + table + "\"", ".*Invalid YDB table path.*");
        }
    }

    @Test
    public void testCaseOnlyTablePathAmbiguity() throws SQLException {
        String suffix = uniqueSuffix();
        String firstDirectory = "Case_" + suffix;
        String secondDirectory = "case_" + suffix;
        String firstTable = firstDirectory + "/Orders";
        String secondTable = secondDirectory + "/orders";
        boolean firstTableCreated = false;
        boolean secondTableCreated = false;

        createDirectory(firstDirectory);
        createDirectory(secondDirectory);
        try {
            createRawTable(firstTable);
            firstTableCreated = true;
            createRawTable(secondTable);
            secondTableCreated = true;

            assertQueryFails(
                    "SELECT * FROM \"" + secondTable + "\"",
                    ".*(?s)Ambiguous.*" + firstTable + ".*" + secondTable + ".*");
        }
        finally {
            if (secondTableCreated) {
                dropRawTable(secondTable);
            }
            if (firstTableCreated) {
                dropRawTable(firstTable);
            }
            removeDirectory(secondDirectory);
            removeDirectory(firstDirectory);
        }
    }

    @Test
    public void testUniqueCaseTableWriteLifecycle() throws SQLException {
        String suffix = uniqueSuffix();
        String remoteTable = "MixedCase_" + suffix;
        String logicalTable = remoteTable.toLowerCase(java.util.Locale.ROOT);
        String renamedTable = "renamed_" + suffix;
        boolean tableCreated = false;
        boolean tableRenamed = false;

        try {
            createRawTable(remoteTable);
            tableCreated = true;

            assertUpdate("INSERT INTO \"" + logicalTable + "\" VALUES (1)", 1);
            assertQuerySucceeds("ALTER TABLE \"" + logicalTable + "\" RENAME TO \"" + renamedTable + "\"");
            tableRenamed = true;

            assertQuery("SELECT id FROM \"" + renamedTable + "\"", "VALUES CAST(1 AS BIGINT)");
            assertThat(computeActual("SHOW TABLES").getOnlyColumnAsSet()).contains(renamedTable);
        }
        finally {
            if (tableCreated) {
                dropRawTable(tableRenamed ? renamedTable : remoteTable);
            }
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

    private static void dropRawTable(String remoteTable) throws SQLException {
        try (Connection connection = DriverManager.getConnection(YdbQueryRunner.buildJdbcUrl(ydb));
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE `" + remoteTable + "`");
        }
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
        // CHAR хранится как String без паддинга
    }

    @Test
    @Override
    public void testVarcharCastToDateInPredicate() {
        // YDB не поддерживает такой pushdown/cast
    }

    @Test
    @Override
    public void testInsertForDefaultColumn() {
        // Requires createTableWithDefaultColumns() which is connector-specific and not supported yet
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
        if (dataMappingTestSetup.getTrinoTypeName().equals("date")) {
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
