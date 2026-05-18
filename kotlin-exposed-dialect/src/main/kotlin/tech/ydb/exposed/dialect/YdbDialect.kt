package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryAlias
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

    override fun hexToDb(hexString: String): String = "String::HexDecode('$hexString')"
}

internal object YdbFunctionProvider : FunctionProvider() {

    private const val MERGE_UNSUPPORTED =
        "YDB dialect does not support ANSI MERGE through Exposed. Use UPSERT or batchUpsert instead."

    override fun random(seed: Int?): String = "Random()"

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

    fun createSecondaryIndex(table: Table, spec: YdbSecondaryIndexSpec): String {
        val tr = TransactionManager.current()
        return buildString {
            append("ALTER TABLE ")
            append(tr.identity(table))
            append(" ADD ")
            append(renderYdbSecondaryIndex(spec))
        }
    }

    override fun dropIndex(
        tableName: String,
        indexName: String,
        isUnique: Boolean,
        isPartialOrFunctional: Boolean
    ): String = "ALTER TABLE $tableName DROP INDEX $indexName"

    fun setTtl(table: YdbTable): String {
        val tr = TransactionManager.current()
        val ttl = table.ttlSettings
            ?: error("TTL is not configured for table ${table.tableName}")

        validateYdbTtlColumn(ttl)
        val normalizedInterval = normalizeTtlInterval(ttl.intervalIso8601)

        return buildString {
            append("ALTER TABLE ")
            append(tr.identity(table))
            append(" SET (TTL = Interval(\"")
            append(normalizedInterval)
            append("\") ON ")
            append(tr.identity(ttl.column))
            ttl.mode.toSql()?.let {
                append(" AS ")
                append(it)
            }
            append(")")
        }
    }

    fun resetTtl(table: YdbTable): String {
        val tr = TransactionManager.current()
        return "ALTER TABLE ${tr.identity(table)} RESET (TTL)"
    }

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
