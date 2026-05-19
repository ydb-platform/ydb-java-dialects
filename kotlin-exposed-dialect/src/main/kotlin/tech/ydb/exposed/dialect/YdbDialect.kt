package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.append
import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryAlias
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.MergeStatement
import org.jetbrains.exposed.v1.core.vendors.DataTypeProvider
import org.jetbrains.exposed.v1.core.vendors.FunctionProvider
import org.jetbrains.exposed.v1.core.vendors.VendorDialect
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.vendors.DatabaseDialectMetadata
import java.sql.Connection
import java.sql.DatabaseMetaData
import kotlin.use

/**
 * Default YDB column type mappings used by Exposed when a column is declared via standard
 * Exposed DSL (`integer`, `varchar`, `date`, ...) — see `YdbColumnTypes.kt` for YDB-specific
 * column types that have no direct Exposed equivalent (e.g. `JsonDocument`, `Interval`, `Uint64`).
 *
 * For `java.time` temporal columns with correct JDBC vendor binding, use
 * [tech.ydb.exposed.dialect.javatime.ydbDate] / [tech.ydb.exposed.dialect.javatime.ydbDate32]
 * (and the other extensions in that package). [ydbInterval] / [ydbInterval64] live in this module root.
 */
internal class YdbDataTypeProvider(
    private val enableSignedDatetimes: Boolean = false
) : DataTypeProvider() {
    override fun booleanType(): String = "Bool"

    override fun byteType(): String = "Int8"
    override fun shortType(): String = "Int16"
    override fun integerType(): String = "Int32"
    override fun longType(): String = "Int64"

    override fun ubyteType(): String = "Uint8"
    override fun ushortType(): String = "Uint16"
    override fun uintegerType(): String = "Uint32"
    override fun ulongType(): String = "Uint64"

    override fun floatType(): String = "Float"
    override fun doubleType(): String = "Double"

    override fun binaryType(): String = "Bytes"
    override fun binaryType(length: Int): String = binaryType()
    override fun blobType(): String = binaryType()

    override fun textType(): String = "Text"
    override fun varcharType(colLength: Int): String = textType()
    override fun mediumTextType(): String = textType()
    override fun largeTextType(): String = textType()

    override fun jsonType(): String = "Json"
    override fun jsonBType(): String = "JsonDocument"

    override fun integerAutoincType(): String = "Serial"
    override fun longAutoincType(): String = "BigSerial"

    override fun uuidType(): String = "Uuid"

    override fun uintegerAutoincType(): String =
        throw UnsupportedOperationException("YDB does not support unsigned Serial columns")
    override fun ulongAutoincType(): String =
        throw UnsupportedOperationException("YDB does not support unsigned Serial columns")

    override fun dateType(): String = if (enableSignedDatetimes) "Date32" else "Date"

    override fun dateTimeType(): String = if (enableSignedDatetimes) "Datetime64" else "Datetime"

    override fun timestampType(): String = if (enableSignedDatetimes) "Timestamp64" else "Timestamp"

    /** YQL literal for binary columns: `Unwrap(String::HexDecode('...'), ...)`. */
    override fun hexToDb(hexString: String): String =
        "Unwrap(String::HexDecode('$hexString'), 'invalid hex bytes literal')"
}

internal object YdbFunctionProvider : FunctionProvider() {

    private const val MERGE_UNSUPPORTED =
        "YDB does not support ANSI MERGE. Use upsert { } / replace { }, UPSERT INTO … , or UPDATE … ON — " +
            "see https://ydb.tech/docs/en/yql/reference/syntax/upsert_into and …/update"

    private const val JSON_CONTAINS_UNSUPPORTED =
        "YDB does not support JSON_CONTAINS. Use JSON_EXISTS or compare JSON_VALUE / JSON_QUERY instead."

    private const val INSERT_VALUE_CLASS = "org.jetbrains.exposed.v1.core.statements.InsertValue"

    /**
     * Maps Exposed [org.jetbrains.exposed.v1.core.Random] to YQL [Random](https://ydb.tech/docs/en/yql/reference/builtins/basic).
     *
     * YDB optional arguments are **not** a reproducible PRNG seed (unlike MySQL `RAND(n)`).
     * They only group call sites inside one query (same arguments → same value in the same execution phase).
     * See YDB docs: `Random(1)` — one draw per query; `Random(column)` — per row.
     *
     * @param seed When `null`, each call site gets an independent `Random()`.
     *   When set, emitted as `Random(seed)` for YDB call grouping only.
     */
    override fun random(seed: Int?): String =
        if (seed == null) "Random()" else "Random($seed)"

    override fun <T : String?> charLength(expr: Expression<T>, queryBuilder: QueryBuilder): Unit = queryBuilder {
        append("Unicode::GetLength(", expr, ")")
    }

    override fun <T : String?> substring(
        expr: Expression<T>,
        start: Expression<Int>,
        length: Expression<Int>,
        builder: QueryBuilder,
        prefix: String
    ): Unit = builder {
        append("Unicode::Substring(", expr, ", ", start, ", ", length, ")")
    }

    override fun concat(separator: String, queryBuilder: QueryBuilder, vararg expr: Expression<*>) {
        if (expr.isEmpty()) {
            queryBuilder { append("''") }
            return
        }
        queryBuilder {
            if (separator.isEmpty()) {
                expr.appendTo(separator = " || ") { +it }
            } else {
                append("Unicode::JoinFromList(AsList(")
                expr.appendTo { append("CAST(", it, " AS Utf8)") }
                append("), '", escapeStringLiteral(separator), "')")
            }
        }
    }

    /**
     * [Unicode::Find](https://ydb.tech/docs/en/yql/reference/udf/list/unicode) is 0-based;
     * Exposed [locate] is 1-based (0 when not found).
     */
    override fun <T : String?> locate(
        queryBuilder: QueryBuilder,
        expr: Expression<T>,
        substring: String
    ) = queryBuilder {
        val needle = escapeStringLiteral(substring)
        append("COALESCE(CAST(Unicode::Find(", expr, ", '", needle, "') + 1u AS Int32), 0)")
    }

    override fun <T : String?> regexp(
        expr1: Expression<T>,
        pattern: Expression<String>,
        caseSensitive: Boolean,
        queryBuilder: QueryBuilder
    ): Unit = queryBuilder {
        if (caseSensitive) {
            append(expr1, " REGEXP ", pattern)
        } else {
            append("Re2::Grep(", pattern, ", Re2::Options(false AS CaseSensitive))(", expr1, ")")
        }
    }

    override fun <T> jsonCast(expression: Expression<T>, jsonType: IColumnType<*>, queryBuilder: QueryBuilder) {
        queryBuilder {
            append("CAST(", expression, " AS ", jsonType.sqlType(), ")")
        }
    }

    override fun <T> jsonExtract(
        expression: Expression<T>,
        vararg path: String,
        toScalar: Boolean,
        jsonType: IColumnType<*>,
        queryBuilder: QueryBuilder
    ) = queryBuilder {
        val jsonPath = buildJsonPath(*path)
        append(if (toScalar) "JSON_VALUE" else "JSON_QUERY")
        append("(", expression, ", '", escapeStringLiteral(jsonPath), "')")
    }

    override fun jsonContains(
        target: Expression<*>,
        candidate: Expression<*>,
        path: String?,
        jsonType: IColumnType<*>,
        queryBuilder: QueryBuilder
    ) {
        throw UnsupportedOperationException(JSON_CONTAINS_UNSUPPORTED)
    }

    override fun jsonExists(
        expression: Expression<*>,
        vararg path: String,
        optional: String?,
        jsonType: IColumnType<*>,
        queryBuilder: QueryBuilder
    ) = queryBuilder {
        val jsonPath = buildJsonPath(*path)
        append("JSON_EXISTS(", expression, ", '", escapeStringLiteral(jsonPath), "'")
        optional?.let { append(" ", it) }
        append(")")
    }

    /**
     * Native YDB [UPSERT](https://ydb.tech/docs/en/yql/reference/syntax/upsert_into): only columns listed in
     * the statement are written; on primary-key conflict, other columns stay unchanged.
     *
     * Exposed `onUpdate` with `insertValue()` (default) maps to the same VALUES. Different insert vs update
     * literals and `onUpdateExclude` are rejected.
     */
    override fun upsert(
        table: Table,
        data: List<Pair<Column<*>, Any?>>,
        expression: String,
        onUpdate: List<Pair<Column<*>, Any?>>,
        keyColumns: List<Column<*>>,
        where: Op<Boolean>?,
        transaction: Transaction
    ): String {
        require(data.isNotEmpty()) { "UPSERT requires at least one column" }
        if (keyColumns.isEmpty()) {
            throw UnsupportedOperationException(
                "YDB UPSERT requires a primary key (or explicit upsert keys); table ${table.tableName} has none"
            )
        }

        validateUpsertOnUpdate(data, onUpdate, keyColumns, transaction)

        if (where != null) {
            throw UnsupportedOperationException(
                "YDB UPSERT does not support Exposed's upsert(where) (PostgreSQL ON CONFLICT … WHERE). " +
                    "Use Table.update { } for conditional updates."
            )
        }

        val columns = data.map { it.first }.distinct()
        val columnList = columns.joinToString(", ") { transaction.identity(it) }
        val dataByColumn = data.toMap()
        val tableName = transaction.identity(table)

        if (expression.isNotBlank()) {
            val valuesExpression = expression.trim()
            val expressionWithColumns = if (valuesExpression.startsWith("VALUES", ignoreCase = true)) {
                "($columnList) $valuesExpression"
            } else {
                valuesExpression
            }
            return "UPSERT INTO $tableName $expressionWithColumns"
        }

        val valueList = columns.joinToString(", ") { column ->
            val value = dataByColumn[column]
            if (value == null) {
                "NULL"
            } else {
                @Suppress("UNCHECKED_CAST")
                (column.columnType as IColumnType<Any?>).valueToString(value)
            }
        }

        return "UPSERT INTO $tableName ($columnList) VALUES ($valueList)"
    }

    /**
     * Native YDB [REPLACE](https://ydb.tech/docs/en/yql/reference/syntax/replace_into): overwrites the row by PK;
     * columns omitted from the statement are reset to table defaults.
     */
    override fun replace(
        table: Table,
        columns: List<Column<*>>,
        expression: String,
        transaction: Transaction,
        prepared: Boolean
    ): String {
        require(columns.isNotEmpty()) { "REPLACE requires at least one column" }
        val columnList = columns.joinToString(", ") { transaction.identity(it) }
        val valuesExpression = expression.trim()
        val expressionWithColumns = if (valuesExpression.startsWith("VALUES", ignoreCase = true)) {
            "($columnList) $valuesExpression"
        } else {
            valuesExpression
        }
        return "REPLACE INTO ${transaction.identity(table)} $expressionWithColumns"
    }

    override fun merge(
        dest: Table,
        source: Table,
        transaction: Transaction,
        clauses: List<MergeStatement.Clause>,
        on: Op<Boolean>?
    ): String = throw UnsupportedOperationException(MERGE_UNSUPPORTED)

    override fun mergeSelect(
        dest: Table,
        source: QueryAlias,
        transaction: Transaction,
        clauses: List<MergeStatement.Clause>,
        on: Op<Boolean>,
        prepared: Boolean
    ): String = throw UnsupportedOperationException(MERGE_UNSUPPORTED)

    override fun queryLimitAndOffset(
        size: Int?,
        offset: Long,
        alreadyOrdered: Boolean
    ): String = buildString {
        if (size != null) {
            append(" LIMIT ")
            append(size)
        }
        if (offset > 0) {
            append(" OFFSET ")
            append(offset)
        }
    }

    private fun validateUpsertOnUpdate(
        data: List<Pair<Column<*>, Any?>>,
        onUpdate: List<Pair<Column<*>, Any?>>,
        keyColumns: List<Column<*>>,
        transaction: Transaction
    ) {
        if (onUpdate.isEmpty()) return

        val dataByColumn = data.toMap()
        val keySet = keyColumns.toSet()

        val conflicting = onUpdate.filter { (column, value) ->
            !isInsertValueExpression(value) && dataByColumn[column] != value
        }
        if (conflicting.isNotEmpty()) {
            val names = conflicting.joinToString { transaction.identity(it.first) }
            throw UnsupportedOperationException(
                "YDB UPSERT applies the same VALUES on insert and on conflict; onUpdate cannot set different " +
                    "values ($names). Use Table.update { } after upsert, or REPLACE for a full-row overwrite " +
                    "(https://ydb.tech/docs/en/yql/reference/syntax/replace_into)."
            )
        }

        val dataCols = data.map { it.first }.toSet()
        val updateCols = onUpdate.map { it.first }.toSet()
        val insertOnly = dataCols - updateCols - keySet
        if (insertOnly.isNotEmpty() && onUpdate.any { !isInsertValueExpression(it.second) }) {
            val names = insertOnly.joinToString { transaction.identity(it) }
            throw UnsupportedOperationException(
                "YDB UPSERT cannot insert column(s) $names while excluding them from the conflict update " +
                    "(Exposed onUpdateExclude). Omit those columns from the upsert body or include them in REPLACE."
            )
        }
    }

    private fun isInsertValueExpression(value: Any?): Boolean =
        value != null && value.javaClass.name == INSERT_VALUE_CLASS

    /** [JsonPath](https://ydb.tech/docs/en/yql/reference/builtins/json) for `JSON_VALUE` / `JSON_QUERY` / `JSON_EXISTS`. */
    private fun buildJsonPath(vararg segments: String): String {
        if (segments.isEmpty()) return "$"
        if (segments.size == 1) {
            val only = segments[0]
            if (only.startsWith("$")) return only
        }

        val path = StringBuilder("$")
        for (segment in segments) {
            if (segment.isEmpty()) continue
            when {
                segment.all(Char::isDigit) -> path.append('[').append(segment).append(']')
                segment.startsWith("[") && segment.endsWith("]") -> path.append(segment)
                else -> {
                    if (path.last() == '$' || path.last() == ']') {
                        path.append('.')
                    }
                    path.append(quoteJsonPathKey(segment))
                }
            }
        }
        return path.toString()
    }

    private fun quoteJsonPathKey(key: String): String =
        if (key.all { it.isLetterOrDigit() || it == '_' }) {
            key
        } else {
            "\"${key.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }

    private fun escapeStringLiteral(value: String): String = value.replace("'", "''")
}

/**
 * Exposed [VendorDialect] for YDB.
 *
 * Use [registerYdbDialect] then `Database.connect("jdbc:ydb:...")`.
 *
 * Notable behavior:
 * - [upsert] / [replace] → native YQL (partial columns vs defaults).
 * - [createIndex] → `ALTER TABLE ... ADD INDEX ... GLOBAL`.
 * - Functional indexes and ANSI `MERGE` are rejected.
 *
 * @property enableSignedDatetimes Passed to [YdbDataTypeProvider] for standard temporal DDL only.
 */
class YdbDialect internal constructor(
    val enableSignedDatetimes: Boolean = false
) : VendorDialect(
    DIALECT_NAME,
    YdbDataTypeProvider(enableSignedDatetimes),
    YdbFunctionProvider
) {
    /**
     * Post-create index: `ALTER TABLE t ADD INDEX i GLOBAL [UNIQUE] ON (cols)`.
     * Use Exposed [Table.index] / [SchemaUtils.create]; indexes are added via `ALTER TABLE … ADD INDEX … GLOBAL`.
     */
    override fun createIndex(index: Index): String {
        val tr = runCatching { TransactionManager.current() }.getOrNull()
        if (!index.functions.isNullOrEmpty()) {
            throw UnsupportedOperationException("YDB dialect does not support functional indexes")
        }

        val columns = index.columns.joinToString(", ") { column ->
            tr?.identity(column) ?: column.name
        }

        val indexName = tr?.db?.identifierManager?.cutIfNecessaryAndQuote(index.indexName) ?: index.indexName
        val tableName = tr?.identity(index.table) ?: index.table.tableName
        val unique = index.unique

        return buildString {
            append("ALTER TABLE ")
            append(tableName)
            append(" ADD INDEX ")
            append(indexName)
            append(" GLOBAL")
            if (unique) {
                append(" UNIQUE")
            }
            append(" ON (")
            append(columns)
            append(")")
        }
    }

    override fun dropIndex(
        tableName: String,
        indexName: String,
        isUnique: Boolean,
        isPartialOrFunctional: Boolean
    ): String = "ALTER TABLE $tableName DROP INDEX $indexName"

    internal companion object {
        const val DIALECT_NAME = "ydb"
    }
}

/** JDBC metadata bridge so Exposed can read existing GLOBAL indexes on YDB. */
internal object YdbDialectMetadata : DatabaseDialectMetadata() {

    override fun existingIndices(vararg tables: Table): Map<Table, List<Index>> {
        val connection = TransactionManager.current().connection.connection as Connection
        val metadata = connection.metaData

        return tables.associateWith { table ->
            readIndices(metadata, table)
        }
    }

    private fun readIndices(metadata: DatabaseMetaData, table: Table): List<Index> {
        val indexColumns = linkedMapOf<String, MutableList<IndexedColumn>>()

        metadata.getIndexInfo(null, null, table.tableName, false, false).use { rs ->
            while (rs.next()) {
                val indexName = rs.getString("INDEX_NAME") ?: continue
                val columnName = rs.getString("COLUMN_NAME") ?: continue

                val column = table.columns.firstOrNull { it.name.equals(columnName, ignoreCase = true) }
                    ?: continue

                val ordinal = rs.getShort("ORDINAL_POSITION").toInt()
                val unique = !rs.getBoolean("NON_UNIQUE")

                indexColumns
                    .getOrPut(indexName) { mutableListOf() }
                    .add(IndexedColumn(column, ordinal, unique))
            }
        }

        return indexColumns.mapNotNull { (indexName, columns) ->
            val orderedColumns = columns
                .sortedWith(compareBy {
                    it.ordinal.takeIf { ordinal -> ordinal > 0 } ?: Int.MAX_VALUE
                })
                .map { it.column }

            if (orderedColumns.isEmpty()) {
                null
            } else {
                Index(
                    columns = orderedColumns,
                    unique = columns.all { it.unique },
                    customName = indexName,
                    indexType = null,
                    filterCondition = null,
                    functions = emptyList(),
                    functionsTable = table
                )
            }
        }
    }

    private data class IndexedColumn(
        val column: Column<*>,
        val ordinal: Int,
        val unique: Boolean
    )
}
