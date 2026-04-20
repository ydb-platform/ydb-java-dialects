package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.VendorDialect
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import tech.ydb.exposed.dialect.functions.YdbFunctionProvider
import tech.ydb.exposed.dialect.types.YdbDataTypeProvider
import kotlin.reflect.full.memberProperties

class YdbDialect : VendorDialect("ydb", YdbDataTypeProvider(), YdbFunctionProvider()) {

    override fun createIndex(index: Index): String {
        val tr = runCatching { TransactionManager.current() }.getOrNull()

        val columns = index.columns.joinToString(", ") { column ->
            tr?.identity(column) ?: column.name
        }

        val indexName = index.indexName
        val tableName = tr?.identity(index.table) ?: index.table.tableName
        val unique = index.extractUniqueFlag()

        require(!unique) {
            "UNIQUE secondary indexes are not supported by the current YDB runtime used by this dialect"
        }

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
        val ttl = table.getTtlSettings()
            ?: error("TTL is not configured for table ${table.tableName}")

        validateTtlColumn(ttl)

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

    private fun validateTtlColumn(ttl: YdbTtlSettings) {
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
    }

    private fun Index.extractUniqueFlag(): Boolean {
        val prop = this::class.memberProperties.firstOrNull {
            it.name == "unique" || it.name == "isUnique"
        } ?: return false

        return (runCatching { prop.getter.call(this) }.getOrNull() as? Boolean) == true
    }
}
