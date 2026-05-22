package tech.ydb.exposed.dialect.integration.ddl

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.createYdbStatement
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest

/**
 * Migration validation for externally managed schemas.
 *
 * In production the schema is usually created by versioned migrations, while Exposed is used to
 * validate that the current database layout still matches the application's table model.
 *
 * Exposed 1.3.0 provides [org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils], but its full
 * JDBC diff path unconditionally queries CHECK-constraint metadata from INFORMATION_SCHEMA.
 * YDB does not expose that metadata through the JDBC driver, so for YDB we validate drift through
 * the compatible schema-diff primitives that Exposed already exposes:
 * - [SchemaUtils.addMissingColumnsStatements] for missing columns;
 * - [tech.ydb.exposed.dialect.YdbDialectMetadata] for existing secondary indexes.
 *
 * We intentionally apply schema changes through raw SQL first and only then ask Exposed for the
 * statements that would be needed to bring the database schema back in sync with the table model.
 */
class MigrationValidationIT : BaseYdbTest() {

    object ExternalUsers : Table("external_users") {
        val id = integer("id")
        val name = varchar("name", 255)
        val email = varchar("email", 255)

        override val primaryKey = PrimaryKey(id)

        init {
            index(false, email)
        }


        override fun createStatement(): List<String> = createYdbStatement()
    }

    @AfterEach
    fun tearDown() {
        transaction(db) {
            runCatching { SchemaUtils.drop(ExternalUsers) }
        }
    }

    @Test
    fun `ydb-compatible schema validation returns no statements for a matching externally created schema`() {
        applySchemaManually(
            """
            CREATE TABLE IF NOT EXISTS external_users (
                id Int32 NOT NULL,
                name Text NOT NULL,
                email Text NOT NULL,
                PRIMARY KEY (id)
            )
            """.trimIndent(),
            "ALTER TABLE external_users ADD INDEX external_users_email GLOBAL ON (email)"
        )

        val statements = requiredMigrationStatements()

        assertTrue(statements.isEmpty(), statements.joinToString(separator = "\n"))
    }

    @Test
    fun `ydb-compatible schema validation reports drift for an incomplete externally created schema`() {
        applySchemaManually(
            """
            CREATE TABLE IF NOT EXISTS external_users (
                id Int32 NOT NULL,
                name Text NOT NULL,
                PRIMARY KEY (id)
            )
            """.trimIndent()
        )

        val statements = requiredMigrationStatements()

        assertTrue(statements.isNotEmpty(), "Expected schema drift to be reported")
        assertTrue(
            statements.any { statement ->
                statement.contains("email", ignoreCase = true) ||
                    statement.contains("ADD INDEX", ignoreCase = true)
            },
            statements.joinToString(separator = "\n")
        )
    }

    /**
     * JetBrains Exposed warns that schema changes performed through raw SQL should be validated
     * in a fresh transaction to avoid stale metadata cache.
     */
    private fun applySchemaManually(vararg statements: String) {
        transaction(db) {
            runCatching { SchemaUtils.drop(ExternalUsers) }
            statements.forEach { statement ->
                exec(statement)
            }
        }
    }

    private fun requiredMigrationStatements(): List<String> =
        transaction(db) {
            buildList {
                addAll(missingColumnStatements())
                addAll(missingIndexStatements())
            }
        }

    private fun missingColumnStatements(): List<String> {
        val existingColumnNames = db.dialectMetadata
            .tableColumns(ExternalUsers)
            .getValue(ExternalUsers)
            .map { metadata -> metadata.name.normalizedIdentifier() }
            .toSet()

        return ExternalUsers.columns
            .filterNot { column -> column.name.normalizedIdentifier() in existingColumnNames }
            .map { column -> "MISSING COLUMN ${ExternalUsers.tableName}.${column.name}" }
    }

    private fun missingIndexStatements(): List<String> {
        val existingIndexNames = db.dialectMetadata
            .existingIndices(ExternalUsers)
            .getValue(ExternalUsers)
            .map { index -> index.indexName.normalizedIdentifier() }
            .toSet()

        return ExternalUsers.indices
            .filterNot { index -> index.indexName.normalizedIdentifier() in existingIndexNames }
            .map { index -> "MISSING INDEX ${index.indexName}" }
    }

    private fun String.normalizedIdentifier(): String = trim('`', '"').lowercase()
}



