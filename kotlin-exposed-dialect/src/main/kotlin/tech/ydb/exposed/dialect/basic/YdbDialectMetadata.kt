package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.vendors.DatabaseDialectMetadata
import java.sql.Connection
import java.sql.DatabaseMetaData

class YdbDialectMetadata : DatabaseDialectMetadata() {

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
                .sortedWith(compareBy<IndexedColumn> { it.ordinal.takeIf { ordinal -> ordinal > 0 } ?: Int.MAX_VALUE })
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
