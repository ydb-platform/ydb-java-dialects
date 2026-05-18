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

/**
 * Default YDB column type mappings used by Exposed when a column is declared via standard
 * Exposed DSL (`integer`, `varchar`, `date`, ...) — see `YdbColumnTypes.kt` for YDB-specific
 * column types that have no direct Exposed equivalent (e.g. `JsonDocument`, `Interval`, `Uint64`).
 *
 * For `java.time` temporal columns with correct JDBC vendor binding, use
 * [tech.ydb.exposed.dialect.javatime.ydbDate] / [tech.ydb.exposed.dialect.javatime.ydbDate32]
 * (and the other extensions in that package). [ydbInterval] / [ydbInterval64] live in this module root.
 */
internal class YdbDataTypeProvider : DataTypeProvider() {
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

    override fun dateType(): String = "Date"
    override fun dateTimeType(): String = "Datetime"
    override fun timestampType(): String = "Timestamp"

    override fun hexToDb(hexString: String): String =
        "Unwrap(String::HexDecode('$hexString'), 'invalid hex bytes literal')"
}

internal object YdbFunctionProvider : FunctionProvider() {

    private const val MERGE_UNSUPPORTED =
        "YDB dialect does not support ANSI MERGE through Exposed. Use UPSERT or batchUpsert instead."

    private const val JSON_CONTAINS_UNSUPPORTED =
        "YDB does not support JSON_CONTAINS. Use JSON_EXISTS or compare JSON_VALUE / JSON_QUERY instead."

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
                append("), '", escapeYqlStringLiteral(separator), "')")
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
        val needle = escapeYqlStringLiteral(substring)
        append("IF(Unicode::Find(", expr, ", '", needle, "') IS NULL, 0, ")
        append("CAST(Unicode::Find(", expr, ", '", needle, "') + 1u AS Int32))")
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
        val jsonPath = buildYdbJsonPath(*path)
        append(if (toScalar) "JSON_VALUE" else "JSON_QUERY")
        append("(", expression, ", '", escapeYqlStringLiteral(jsonPath), "')")
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
        val jsonPath = buildYdbJsonPath(*path)
        append("JSON_EXISTS(", expression, ", '", escapeYqlStringLiteral(jsonPath), "'")
        optional?.let { append(" ", it) }
        append(")")
    }

    override fun upsert(
        table: Table,
        data: List<Pair<Column<*>, Any?>>,
        expression: String,
        onUpdate: List<Pair<Column<*>, Any?>>,
        keyColumns: List<Column<*>>,
        where: Op<Boolean>?,
        transaction: Transaction
    ): String = renderUpsertOrReplace("UPSERT", table, data, expression, where, transaction)

    /**
     * YDB has native `REPLACE INTO` which has the same write semantics as INSERT-or-overwrite
     * (key is the primary key, no need for an extra unique constraint).
     */
    override fun replace(
        table: Table,
        columns: List<Column<*>>,
        expression: String,
        transaction: Transaction,
        prepared: Boolean
    ): String {
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

    private fun renderUpsertOrReplace(
        operation: String,
        table: Table,
        data: List<Pair<Column<*>, Any?>>,
        expression: String,
        where: Op<Boolean>?,
        transaction: Transaction
    ): String {
        require(where == null) {
            "YDB $operation does not support WHERE clause"
        }

        val columnList = data.joinToString(", ") { (column, _) ->
            transaction.identity(column)
        }

        if (expression.isNotBlank()) {
            val valuesExpression = expression.trim()
            val expressionWithColumns = if (valuesExpression.startsWith("VALUES", ignoreCase = true)) {
                "($columnList) $valuesExpression"
            } else {
                valuesExpression
            }
            return "$operation INTO ${transaction.identity(table)} $expressionWithColumns"
        }

        val valueList = data.joinToString(", ") { (column, value) ->
            valueToSqlLiteral(column, value)
        }

        return "$operation INTO ${transaction.identity(table)} ($columnList) VALUES ($valueList)"
    }

    @Suppress("UNCHECKED_CAST")
    private fun valueToSqlLiteral(column: Column<*>, value: Any?): String {
        if (value == null) return "NULL"

        val columnType = column.columnType as IColumnType<Any?>
        return columnType.valueToString(value)
    }
}

/**
 * Exposed [VendorDialect] for YDB.
 *
 * Usually obtained via [connectYdb], which wires it into a [Database] together
 * with a default [org.jetbrains.exposed.v1.core.DatabaseConfig] tuned for YDB
 * (SERIALIZABLE isolation, nested transactions disabled).
 */
class YdbDialect internal constructor() : VendorDialect(
    DIALECT_NAME,
    YdbDataTypeProvider(),
    YdbFunctionProvider
) {
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

    internal object Metadata : DatabaseDialectMetadata() {

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
                    .sortedWith(compareBy<IndexedColumn> {
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

    internal companion object {
        const val DIALECT_NAME = "ydb"
    }
}
