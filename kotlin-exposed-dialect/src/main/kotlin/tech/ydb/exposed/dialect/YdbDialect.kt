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
import kotlin.use


internal object YdbDataTypeProvider : DataTypeProvider() {
    override fun byteType(): String = "Int8"
    override fun ubyteType(): String = "Uint8"

    override fun binaryType(): String = "String"
    override fun binaryType(length: Int): String = "String"

    override fun blobType(): String = binaryType()

    override fun hexToDb(hexString: String): String = "'$hexString'"

    override fun shortType(): String = "Int16"
    override fun ushortType(): String = "Uint16"

    override fun integerType(): String = "Int32"
    override fun uintegerType(): String = "Uint32"

    override fun integerAutoincType(): String =
        throw UnsupportedOperationException(
            "YDB does not support AUTO_INCREMENT. Use YdbUuidIdTable, YdbUuidStringIdTable, or YdbUlidTable instead."
        )

    override fun longType(): String = "Int64"
    override fun booleanType(): String = "Bool"

    override fun floatType(): String = "Float"
    override fun doubleType(): String = "Double"

    override fun varcharType(colLength: Int): String = "Text"
    override fun textType(): String = "Text"
    override fun mediumTextType(): String = textType()
    override fun largeTextType(): String = textType()

    override fun uuidType(): String = "Uuid"

    override fun dateType(): String = "Date32"
    override fun dateTimeType(): String = "Datetime64"
    override fun timestampType(): String = "Timestamp64"

    override fun jsonType(): String = "JsonDocument"
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
    ): String {
        require(where == null) {
            "YDB UPSERT does not support WHERE clause in this dialect implementation"
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

            return "UPSERT INTO ${transaction.identity(table)} $expressionWithColumns"
        }

        val valueList = data.joinToString(", ") { (column, value) ->
            valueToSqlLiteral(column, value)
        }

        return "UPSERT INTO ${transaction.identity(table)} ($columnList) VALUES ($valueList)"
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

    @Suppress("UNCHECKED_CAST")
    private fun valueToSqlLiteral(column: Column<*>, value: Any?): String {
        if (value == null) return "NULL"

        val columnType = column.columnType as IColumnType<Any?>
        return columnType.valueToString(value)
    }
}

class YdbDialect : VendorDialect("ydb", YdbDataTypeProvider, YdbFunctionProvider) {

    override fun createIndex(index: Index): String {
        val tr = runCatching { TransactionManager.Companion.current() }.getOrNull()
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
        val tr = TransactionManager.Companion.current()
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

        return buildString {
            append("ALTER TABLE ")
            append(tr.identity(table))
            append(" SET (TTL = Interval(\"")
            append(ttl.intervalIso8601)
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
}

