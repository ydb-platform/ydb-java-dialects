package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.vendors.VendorDialect
import tech.ydb.exposed.dialect.types.YdbDataTypeProvider
import tech.ydb.exposed.dialect.functions.YdbFunctionProvider
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

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

    fun setTtl(table: YdbTable): String {
        val tr = TransactionManager.current()
        val ttl = table.getTtlSettings()
            ?: error("TTL is not configured for table ${table.tableName}")

        val sqlType = ttl.column.columnType.sqlType()
        val supported = when (ttl.mode) {
            YdbTtlColumnMode.DATE_TYPE ->
                sqlType == "Date" || sqlType == "Datetime" || sqlType == "Timestamp"

            YdbTtlColumnMode.SECONDS,
            YdbTtlColumnMode.MILLISECONDS,
            YdbTtlColumnMode.MICROSECONDS,
            YdbTtlColumnMode.NANOSECONDS ->
                sqlType == "Uint32" || sqlType == "Uint64" || sqlType == "DyNumber"
        }

        require(supported) {
            "YDB TTL does not support column '${ttl.column.name}' of type '$sqlType' for mode '${ttl.mode}'"
        }

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