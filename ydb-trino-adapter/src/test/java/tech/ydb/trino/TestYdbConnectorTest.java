package tech.ydb.trino;

import io.trino.Session;
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

import static io.trino.spi.StandardErrorCode.NOT_SUPPORTED;
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
    public void testNestedTablePath() throws Exception {
        String namespace = "namespace_" + uniqueSuffix();
        String directory = namespace + "/eu";
        String table = directory + "/orders";

        createDirectory(directory);
        try (AutoCloseable ignoredNamespace = () -> removeDirectory(namespace);
                AutoCloseable ignoredDirectory = () -> removeDirectory(directory);
                AutoCloseable ignoredTable = () -> assertQuerySucceeds("DROP TABLE IF EXISTS \"" + table + "\"")) {
            assertQuerySucceeds("CREATE TABLE \"" + table + "\" (id bigint NOT NULL)");
            assertThat(computeActual("SHOW TABLES").getOnlyColumnAsSet()).contains(table);
            assertQueryReturnsEmptyResult("SELECT id FROM \"" + table + "\"");
            assertThat(computeScalar("SHOW CREATE TABLE \"" + table + "\"").toString()).contains(table);
        }
    }

    @Test
    public void testNestedTablePathLongerThanSingleComponentLimit() throws Exception {
        String suffix = uniqueSuffix();
        String parent = "long_" + "a".repeat(130) + suffix;
        String directory = parent + "/branch_" + "b".repeat(90);
        String sourceTable = directory + "/source_" + suffix;
        String renamedTable = directory + "/renamed_" + suffix;

        assertThat(sourceTable.length()).isGreaterThan(255);
        createDirectory(directory);
        try (AutoCloseable ignoredParent = () -> removeDirectory(parent);
                AutoCloseable ignoredDirectory = () -> removeDirectory(directory);
                AutoCloseable ignoredSourceTable = () -> assertQuerySucceeds("DROP TABLE IF EXISTS \"" + sourceTable + "\"");
                AutoCloseable ignoredRenamedTable = () -> assertQuerySucceeds("DROP TABLE IF EXISTS \"" + renamedTable + "\"")) {
            assertQuerySucceeds("CREATE TABLE \"" + sourceTable + "\" (id bigint NOT NULL)");
            assertThat(computeActual("SHOW TABLES").getOnlyColumnAsSet()).contains(sourceTable);
            assertQueryReturnsEmptyResult("SELECT id FROM \"" + sourceTable + "\"");

            assertQuerySucceeds("ALTER TABLE \"" + sourceTable + "\" RENAME TO \"" + renamedTable + "\"");
            assertThat(computeActual("SHOW TABLES").getOnlyColumnAsSet())
                    .contains(renamedTable)
                    .doesNotContain(sourceTable);
            assertQueryReturnsEmptyResult("SELECT id FROM \"" + renamedTable + "\"");
        }
    }

    @Test
    public void testDefaultSchemaRelationComments() throws Exception {
        String namespace = "comments_" + uniqueSuffix();
        String directory = namespace + "/eu";
        String table = directory + "/orders";

        createDirectory(directory);
        try (AutoCloseable ignoredNamespace = () -> removeDirectory(namespace);
                AutoCloseable ignoredDirectory = () -> removeDirectory(directory);
                AutoCloseable ignoredTable = () -> assertQuerySucceeds("DROP TABLE IF EXISTS \"" + table + "\"")) {
            assertQuerySucceeds("CREATE TABLE \"" + table + "\" (id bigint NOT NULL)");

            assertThat(query(
                    "SELECT schema_name, table_name, comment " +
                            "FROM system.metadata.table_comments " +
                            "WHERE catalog_name = 'ydb' AND schema_name = 'default'"))
                    .skippingTypesCheck()
                    .containsAll("VALUES ('default', '" + table + "', null)");
            assertQueryReturnsEmptyResult(
                    "SELECT table_name FROM system.metadata.table_comments " +
                            "WHERE catalog_name = 'ydb' AND schema_name = 'missing'");
        }
    }

    @Test
    public void testDefaultSchemaBulkColumns() throws Exception {
        String namespace = "columns_" + uniqueSuffix();
        String directory = namespace + "/eu";
        String table = directory + "/orders";
        Session bulkSession = Session.builder(getSession())
                .setCatalogSessionProperty("ydb", "bulk_list_columns", "true")
                .build();

        createDirectory(directory);
        try (AutoCloseable ignoredNamespace = () -> removeDirectory(namespace);
                AutoCloseable ignoredDirectory = () -> removeDirectory(directory);
                AutoCloseable ignoredTable = () -> assertQuerySucceeds("DROP TABLE IF EXISTS \"" + table + "\"")) {
            createRawMetadataTable(table);

            assertQuery(
                    bulkSession,
                    "SELECT column_name, data_type, is_nullable " +
                            "FROM information_schema.columns " +
                            "WHERE table_schema = 'default' AND table_name = '" + table + "' " +
                            "ORDER BY ordinal_position",
                    "VALUES ('id', 'bigint', 'NO'), ('note', 'varchar', 'YES')");
            assertQueryReturnsEmptyResult(
                    bulkSession,
                    "SELECT column_name FROM information_schema.columns " +
                            "WHERE table_schema = 'missing' AND table_name = '" + table + "'");
        }
    }

    @Test
    public void testInvalidTablePaths() {
        for (String table : List.of("/orders", "a//orders", "a/../orders", ".sys/orders")) {
            assertQueryFails("SELECT * FROM ydb.default.\"" + table + "\"", ".*Invalid YDB table path.*");
        }
    }

    @Test
    public void testCaseOnlyTablePathAmbiguity() throws Exception {
        String suffix = uniqueSuffix();
        String firstDirectory = "Case_" + suffix;
        String secondDirectory = "case_" + suffix;
        String firstTable = firstDirectory + "/Orders";
        String secondTable = secondDirectory + "/orders";

        createDirectory(firstDirectory);
        try (AutoCloseable ignoredFirstDirectory = () -> removeDirectory(firstDirectory)) {
            createDirectory(secondDirectory);
            try (AutoCloseable ignoredSecondDirectory = () -> removeDirectory(secondDirectory)) {
                createRawTable(firstTable);
                try (AutoCloseable ignoredFirstTable = () -> dropRawTable(firstTable)) {
                    createRawTable(secondTable);
                    try (AutoCloseable ignoredSecondTable = () -> dropRawTable(secondTable)) {
                        assertQueryFails(
                                "SELECT * FROM \"" + secondTable + "\"",
                                ".*(?s)Ambiguous.*" + firstTable + ".*" + secondTable + ".*");
                        Session bulkSession = Session.builder(getSession())
                                .setCatalogSessionProperty("ydb", "bulk_list_columns", "true")
                                .build();
                        assertQueryFails(
                                bulkSession,
                                "SELECT table_name FROM information_schema.columns WHERE table_schema = 'default'",
                                ".*(?s)Ambiguous.*" + firstTable + ".*" + secondTable + ".*");
                        assertQueryFails(
                                "SELECT table_name FROM system.metadata.table_comments " +
                                        "WHERE catalog_name = 'ydb' AND schema_name = 'default'",
                                ".*(?s)Ambiguous.*" + firstTable + ".*" + secondTable + ".*");
                    }
                }
            }
        }
    }

    @Test
    public void testUniqueCaseTableWriteLifecycle() throws Exception {
        String suffix = uniqueSuffix();
        String remoteTable = "MixedCase_" + suffix;
        String logicalTable = remoteTable.toLowerCase(java.util.Locale.ROOT);
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
    public void testMergeUsesCompleteCompositePrimaryKeyWithNonKeyFirstColumn() throws Exception {
        String table = "merge_composite_key_" + uniqueSuffix();

        createRawCompositePrimaryKeyTable(table);
        try {
            assertUpdate("INSERT INTO \"" + table + "\" (bucket, account, region, note) VALUES " +
                    "('shared', 1, 10, 'old'), " +
                    "('shared', 1, 20, 'sibling'), " +
                    "('shared', 2, 10, 'cross-sibling'), " +
                    "('shared', 2, 20, 'remove'), " +
                    "('keep', 3, 30, 'stable')", 5);

            assertUpdate("""
                    MERGE INTO "%s" target
                    USING (VALUES
                        (BIGINT '1', BIGINT '10', 'updated', 'update'),
                        (BIGINT '2', BIGINT '20', 'unused', 'delete'),
                        (BIGINT '4', BIGINT '40', 'inserted', 'insert'))
                        AS source(account, region, note, operation)
                    ON target.account = source.account AND target.region = source.region
                    WHEN MATCHED AND source.operation = 'delete' THEN DELETE
                    WHEN MATCHED THEN UPDATE SET bucket = 'shared', note = source.note
                    WHEN NOT MATCHED THEN INSERT (bucket, account, region, note)
                        VALUES ('shared', source.account, source.region, source.note)
                    """.formatted(table), 3);

            assertQuery(
                    "SELECT bucket, account, region, note FROM \"" + table + "\" ORDER BY account, region",
                    "VALUES " +
                            "('shared', CAST(1 AS BIGINT), CAST(10 AS BIGINT), 'updated'), " +
                            "('shared', CAST(1 AS BIGINT), CAST(20 AS BIGINT), 'sibling'), " +
                            "('shared', CAST(2 AS BIGINT), CAST(10 AS BIGINT), 'cross-sibling'), " +
                            "('keep', CAST(3 AS BIGINT), CAST(30 AS BIGINT), 'stable'), " +
                            "('shared', CAST(4 AS BIGINT), CAST(40 AS BIGINT), 'inserted')");
        } finally {
            dropRawTable(table);
        }
    }

    @Test
    public void testPhysicalPrimaryKeyUpdatesAreRejectedWithoutMutation() throws Exception {
        String table = "update_physical_key_" + uniqueSuffix();
        String unchangedRows = "VALUES " +
                "('shared', CAST(1 AS BIGINT), CAST(10 AS BIGINT), 'first'), " +
                "('shared', CAST(2 AS BIGINT), CAST(20 AS BIGINT), 'second')";

        createRawCompositePrimaryKeyTable(table);
        try {
            assertUpdate("INSERT INTO \"" + table + "\" (bucket, account, region, note) VALUES " +
                    "('shared', 1, 10, 'first'), " +
                    "('shared', 2, 20, 'second')", 2);

            assertThat(query("UPDATE \"" + table + "\" SET account = 11 WHERE account = 1 AND region = 10"))
                    .failure()
                    .hasErrorCode(NOT_SUPPORTED)
                    .hasMessageContaining("Cannot update YDB primary key column: account");
            assertQuery(
                    "SELECT bucket, account, region, note FROM \"" + table + "\" ORDER BY account, region",
                    unchangedRows);

            assertThat(query("""
                    MERGE INTO "%s" target
                    USING (VALUES (BIGINT '1', BIGINT '10')) AS source(account, region)
                    ON target.account = source.account AND target.region = source.region
                    WHEN MATCHED THEN UPDATE SET region = target.region + 10, account = target.account + 10
                    """.formatted(table)))
                    .failure()
                    .hasErrorCode(NOT_SUPPORTED)
                    .hasMessageContaining("Cannot update YDB primary key columns: account, region");
            assertQuery(
                    "SELECT bucket, account, region, note FROM \"" + table + "\" ORDER BY account, region",
                    unchangedRows);
        } finally {
            dropRawTable(table);
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

    private static void createRawCompositePrimaryKeyTable(String remoteTable) throws SQLException {
        try (Connection connection = DriverManager.getConnection(YdbQueryRunner.buildJdbcUrl(ydb));
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE `" + remoteTable + "` " +
                    "(bucket Utf8, account Int64 NOT NULL, region Int64 NOT NULL, note Utf8, " +
                    "PRIMARY KEY (account, region))");
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

    @Test
    @Override
    public void testRenameTableToLongTableName() {
        skipTestUnless(hasBehavior(TestingConnectorBehavior.SUPPORTS_RENAME_TABLE));

        String sourceTableName = "test_rename_source_" + uniqueSuffix();
        String baseTableName = "test_rename_target_" + uniqueSuffix();
        int maxLength = maxTableRenameLength().orElseThrow();
        String validTargetTableName = baseTableName + "z".repeat(maxLength - baseTableName.length());
        String invalidTargetTableName = validTargetTableName + "z";

        try {
            assertUpdate("CREATE TABLE " + sourceTableName + " AS SELECT 123 x", 1);
            assertUpdate("ALTER TABLE " + sourceTableName + " RENAME TO " + validTargetTableName);
            assertThat(getQueryRunner().tableExists(getSession(), validTargetTableName)).isTrue();
            assertQuery("SELECT x FROM " + validTargetTableName, "VALUES 123");
            assertUpdate("DROP TABLE " + validTargetTableName);

            assertUpdate("CREATE TABLE " + sourceTableName + " AS SELECT 123 x", 1);
            assertThatThrownBy(() -> assertUpdate("ALTER TABLE " + sourceTableName + " RENAME TO " + invalidTargetTableName))
                    .satisfies(this::verifyTableNameLengthFailurePermissible);
            assertThat(getQueryRunner().tableExists(getSession(), sourceTableName)).isTrue();
            assertQuery("SELECT x FROM " + sourceTableName, "VALUES 123");

            // Trino 479 expects tableExists(invalidTarget) to be false, but this lookup returns INVALID_ARGUMENTS.
            assertQueryFails("SELECT x FROM " + invalidTargetTableName, ".*Invalid YDB table path.*too long.*");
        } finally {
            assertQuerySucceeds("DROP TABLE IF EXISTS " + validTargetTableName);
            assertQuerySucceeds("DROP TABLE IF EXISTS " + sourceTableName);
        }
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
