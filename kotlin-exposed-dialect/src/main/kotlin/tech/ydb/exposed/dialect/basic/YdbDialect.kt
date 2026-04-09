package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnDiff
import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.DataTypeProvider
import org.jetbrains.exposed.v1.core.vendors.DatabaseDialect
import org.jetbrains.exposed.v1.core.vendors.FunctionProvider
import org.jetbrains.exposed.v1.core.vendors.VendorDialect
import tech.ydb.exposed.dialect.types.YdbDataTypeProvider
import tech.ydb.exposed.dialect.functions.YdbFunctionProvider

class YdbDialect: VendorDialect("ydb", YdbDataTypeProvider(), YdbFunctionProvider()) {

//    override val name: String = "ydb"

//    override fun addPrimaryKey(
//        table: Table,
//        pkName: String?,
//        vararg pkColumns: Column<*>
//    ): String {
//        val columns = pkColumns.joinToString(", ") { it.name }
//        return "PRIMARY KEY ($columns)"
//    }



    override fun createIndex(index: Index): String {
        val columns = index.columns.joinToString(", ") { it.name }
        val indexName = index.indexName
        val tableName = index.table.tableName

        return buildString {
            append("ALTER TABLE ")
            append(tableName)
            append(" ADD INDEX ")
            append(indexName)
            append(" GLOBAL ON (")
            append(columns)
            append(")")
        }
    }

    override fun dropIndex(
        tableName: String,
        indexName: String,
        isUnique: Boolean,
        isPartialOrFunctional: Boolean
    ): String {
        return "ALTER TABLE $tableName DROP INDEX $indexName"
    }

//    /**
//     * YDB не поддерживает ALTER COLUMN напрямую.
//     * Обычно требуется пересоздание таблицы.
//     */
//    override fun modifyColumn(
//        column: Column<*>,
//        columnDiff: ColumnDiff
//    ): List<String> {
//        return emptyList()
//    }
//
//
//    override val dataTypeProvider: DataTypeProvider = YdbDataTypeProvider()
//
//    override val functionProvider: FunctionProvider = YdbFunctionProvider()

}