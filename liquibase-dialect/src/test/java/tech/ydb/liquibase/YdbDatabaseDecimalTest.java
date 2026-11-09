package tech.ydb.liquibase;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.command.CommandScope;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.DirectoryResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YdbDatabaseDecimalTest extends BaseTest {

    @Test
    void migrateLoadAndRegenerateParameterizedDecimals(@TempDir Path tempDir) throws Exception {
        migrateChangeFile("./changelogs/decimal.xml");

        try (Connection connection = DriverManager.getConnection(jdbcUrl())) {
            assertDecimalTypes(connection);
            try (Statement statement = connection.createStatement();
                    ResultSet rows = statement.executeQuery("SELECT * FROM decimal_values ORDER BY id")) {
                for (int id = 1; id <= 5; id++) {
                    assertTrue(rows.next());
                    assertEquals(id, rows.getInt("id"));
                    if (id == 4) {
                        assertNull(rows.getBigDecimal("amount"));
                        assertNull(rows.getBigDecimal("whole_amount"));
                        assertNull(rows.getBigDecimal("fraction"));
                        assertNull(rows.getBigDecimal("default_amount"));
                        assertNull(rows.getBigDecimal("small_amount"));
                        assertNull(rows.getBigDecimal("precision_only"));
                    } else {
                        String sign = id == 2 || id == 3 ? "-" : "";
                        assertEquals(new BigDecimal(sign + "1234567890123456789012345.1234567890"),
                                rows.getBigDecimal("amount"));
                        assertEquals(new BigDecimal(sign + "12345678901234567890123456789012345"),
                                rows.getBigDecimal("whole_amount"));
                        assertEquals(new BigDecimal(sign + "0.12345678901234567890123456789012345"),
                                rows.getBigDecimal("fraction"));
                        assertEquals(new BigDecimal(sign + "1234567890123.123456789"),
                                rows.getBigDecimal("default_amount"));
                        assertEquals(new BigDecimal(sign + "9"), rows.getBigDecimal("small_amount"));
                        assertEquals(new BigDecimal(sign + "1234567890"), rows.getBigDecimal("precision_only"));
                    }
                }
                assertFalse(rows.next());
            }
        }

        Path changelogFile = tempDir.resolve("decimal-generated.xml");
        new CommandScope("generateChangelog")
                .addArgumentValue("url", jdbcUrl())
                .addArgumentValue("changelogFile", changelogFile.toString())
                .addArgumentValue("includeObjects", "table:decimal_values")
                .execute();
        String changelog = new String(Files.readAllBytes(changelogFile), StandardCharsets.UTF_8);
        assertTrue(changelog.contains("type=\"DECIMAL(35,10)\""));
        assertTrue(changelog.contains("type=\"DECIMAL(35,0)\""));
        assertTrue(changelog.contains("type=\"DECIMAL(35,35)\""));
        assertTrue(changelog.contains("type=\"DECIMAL(22,9)\""));
        assertTrue(changelog.contains("type=\"DECIMAL(1,0)\""));
        assertTrue(changelog.contains("type=\"DECIMAL(10,0)\""));

        try (Connection connection = DriverManager.getConnection(jdbcUrl());
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE decimal_values");
        }
        try (Connection connection = DriverManager.getConnection(jdbcUrl());
                DirectoryResourceAccessor accessor = new DirectoryResourceAccessor(tempDir);
                Liquibase liquibase = new Liquibase(changelogFile.getFileName().toString(), accessor,
                        new JdbcConnection(connection))) {
            liquibase.update(new Contexts());
            assertDecimalTypes(connection);
        }
    }

    private static void assertDecimalTypes(Connection connection) throws Exception {
        Map<String, String> types = new LinkedHashMap<>();
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, "decimal_values", null)) {
            while (columns.next()) {
                types.put(columns.getString("COLUMN_NAME"), columns.getString("TYPE_NAME"));
            }
        }
        assertEquals("Decimal(35, 10)", types.get("amount"));
        assertEquals("Decimal(35, 0)", types.get("whole_amount"));
        assertEquals("Decimal(35, 35)", types.get("fraction"));
        assertEquals("Decimal(22, 9)", types.get("default_amount"));
        assertEquals("Decimal(1, 0)", types.get("small_amount"));
        assertEquals("Decimal(10, 0)", types.get("precision_only"));
    }
}
