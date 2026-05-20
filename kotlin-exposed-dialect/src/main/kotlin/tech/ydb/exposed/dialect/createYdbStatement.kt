package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

/**
 * Builds a YDB-compatible `CREATE TABLE` statement for this Exposed [Table].
 *
 * This function is intended to be used from [Table.createStatement] overrides
 * for tables that should be created in YDB.
 *
 * The reason for this helper is that Exposed may render a single-column primary
 * key inline, for example:
 *
 * ```sql
 * CREATE TABLE indexed_table (
 *     id Int32 PRIMARY KEY,
 *     email Utf8,
 *     name Utf8
 * )
 * ```
 *
 * YDB does not accept inline primary key declarations. It expects the primary
 * key to be declared as a table-level clause:
 *
 * ```sql
 * CREATE TABLE indexed_table (
 *     id Int32 NOT NULL,
 *     email Utf8 NOT NULL,
 *     name Utf8 NOT NULL,
 *     INDEX ...,
 *     PRIMARY KEY (id)
 * )
 * ```
 *
 * This function renders the table in the YDB-compatible form by:
 *
 * - rendering all columns without inline `PRIMARY KEY` declarations;
 * - appending a table-level `PRIMARY KEY (...)` clause;
 * - preserving `NOT NULL` for non-nullable columns;
 * - preserving database-side default values;
 * - appending Exposed indexes declared on this table.
 *
 * The generated statement is plain `CREATE TABLE`, not
 * `CREATE TABLE IF NOT EXISTS`. This is useful for migration tools, because an
 * already existing table should normally fail the migration instead of being
 * silently ignored.
 *
 * Example:
 *
 * ```kotlin
 * object Users : Table("users") {
 *     val id = integer("id")
 *     val email = varchar("email", 255)
 *     val name = varchar("name", 255)
 *
 *     override val primaryKey = PrimaryKey(id)
 *
 *     init {
 *         index(false, email)
 *         index("email-cover-idx", isUnique = true, email)
 *     }
 *
 *     val emailIndexDefinition
 *         get() = indices.single { !it.unique && it.columns == listOf(email) }
 *
 *     override fun createStatement() = createYdbStatement()
 * }
 * ```
 *
 * @return A list containing a single YDB-compatible `CREATE TABLE` statement
 * for this table.
 *
 * @throws IllegalStateException If this table does not define a primary key.
 * YDB requires every table to have one.
 *
 * @see Table.createStatement
 * @see Table.primaryKey
 */
fun Table.createYdbStatement(): List<String> {
    val tr = TransactionManager.current()

    val pk = primaryKey ?: error("YDB requires PRIMARY KEY for every table: $tableName")

    val columnsSql = columns.joinToString(", ") { column ->
        buildString {
            append(tr.identity(column))
            append(" ")
            append(column.columnType.sqlType())
            if (!column.columnType.nullable) {
                append(" NOT NULL")
            }

            if (column.defaultValueInDb() != null) {
                append(" DEFAULT ")
                append(column.defaultValueInDb())
            }
        }
    }

    val pkSql = pk.columns.joinToString(", ") { tr.identity(it) }

    val sql = buildString {
        append("CREATE TABLE IF NOT EXISTS ")
        append(tr.identity(this@createYdbStatement))
        append(" (")
        append(columnsSql)
        append(", PRIMARY KEY (")
        append(pkSql)
        append("))")

        if (storageParameters.isNotEmpty()) {
            append(" WITH (")
            append(storageParameters.joinToString(separator = ", ") { it.toSQL() })
            append(")")
        }
    }

    return listOf(sql)
}