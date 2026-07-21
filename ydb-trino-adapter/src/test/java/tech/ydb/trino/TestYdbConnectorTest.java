package tech.ydb.trino;

import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;
import io.trino.testing.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tech.ydb.test.junit5.YdbHelperExtension;

import java.util.Optional;

public class TestYdbConnectorTest extends BaseConnectorTest {

    @RegisterExtension
    static final YdbHelperExtension ydb = new YdbHelperExtension();

    @Override
    protected QueryRunner createQueryRunner() throws Exception {
        return YdbQueryRunner.builder(ydb)
                .setInitialTables(REQUIRED_TPCH_TABLES)
                .build();
    }

    @Override
    protected boolean hasBehavior(TestingConnectorBehavior connectorBehavior) {
        return switch (connectorBehavior) {
            case SUPPORTS_MERGE,
                 SUPPORTS_UPDATE,
                 SUPPORTS_DELETE,
                 SUPPORTS_CREATE_VIEW,
                 SUPPORTS_CREATE_SCHEMA,
                 SUPPORTS_RENAME_SCHEMA,
                 SUPPORTS_SET_COLUMN_TYPE,
                 SUPPORTS_DROP_COLUMN,
                 SUPPORTS_ROW_TYPE,
                 SUPPORTS_RENAME_COLUMN,
                 SUPPORTS_ROW_LEVEL_UPDATE,
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
                 SUPPORTS_DROP_NOT_NULL_CONSTRAINT,
                 SUPPORTS_PREDICATE_PUSHDOWN_WITH_VARCHAR_INEQUALITY,
                 SUPPORTS_LIMIT_PUSHDOWN -> false;
            default -> super.hasBehavior(connectorBehavior);
        };
    }

    @Test
    @Override
    public void testCreateTableWithLongTableName() {
        // YDB не поддерживает длинные названия таблиц
    }

    @Test
    @Override
    public void testRenameTableToLongTableName() {
        // YDB не поддерживает длинные названия таблиц
    }

    @Test
    @Override
    public void testAlterTableAddLongColumnName() {
        // YDB не поддерживает длинные названия колонок
    }

    @Test
    @Override
    public void testCreateTableWithLongColumnName() {
        // YDB не поддерживает длинные названия колонок
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
    public void verifySupportsRowLevelUpdateDeclaration() {
        // Planner fails with IllegalArgumentException before connector NOT_SUPPORTED path
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
}
