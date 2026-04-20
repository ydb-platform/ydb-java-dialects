package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

abstract class YdbIdTable<T : Any>(name: String = "") : IdTable<T>(name) {

    private var ttlSettings: YdbTtlSettings? = null
    private val ydbSecondaryIndices = mutableListOf<YdbSecondaryIndexSpec>()

    protected fun ttl(
        column: Column<*>,
        intervalIso8601: String,
        mode: YdbTtlColumnMode = YdbTtlColumnMode.DATE_TYPE
    ) {
        ttlSettings = YdbTtlSettings(column, intervalIso8601, mode)
    }

    protected fun secondaryIndex(
        name: String,
        vararg columns: Column<*>,
        unique: Boolean = false,
        scope: YdbIndexScope = YdbIndexScope.GLOBAL,
        syncMode: YdbIndexSyncMode = YdbIndexSyncMode.SYNC,
        indexType: String? = null,
        coverColumns: List<Column<*>> = emptyList(),
        withParams: Map<String, Any> = emptyMap()
    ) {
        require(columns.isNotEmpty()) { "YDB secondary index must contain at least one column" }

        ydbSecondaryIndices += YdbSecondaryIndexSpec(
            name = name,
            columns = columns.toList(),
            unique = unique,
            scope = scope,
            syncMode = syncMode,
            indexType = indexType,
            coverColumns = coverColumns,
            withParams = withParams
        )
    }

    fun getTtlSettings(): YdbTtlSettings? = ttlSettings

    fun getYdbSecondaryIndices(): List<YdbSecondaryIndexSpec> =
        ydbSecondaryIndices.toList()

    override fun createStatement(): List<String> {
        val tr = TransactionManager.current()

        val pk = primaryKey
            ?: error("YDB requires PRIMARY KEY for every table: $tableName")

        val columnsSql = columns.joinToString(", ") { column ->
            buildString {
                append(tr.identity(column))
                append(" ")
                append(column.columnType.sqlType())

                if (!column.columnType.nullable) {
                    append(" NOT NULL")
                }
            }
        }

        val indexesSql = ydbSecondaryIndices.joinToString(", ") { spec ->
            renderYdbSecondaryIndex(spec)
        }

        val pkSql = pk.columns.joinToString(", ") { tr.identity(it) }

        val ttlSql = ttlSettings?.let { ttl ->
            validateTtlColumn(ttl)

            buildString {
                append(" WITH (TTL = Interval(\"")
                append(ttl.intervalIso8601)
                append("\") ON ")
                append(tr.identity(ttl.column))
                ttl.mode.toSql()?.let {
                    append(" AS ")
                    append(it)
                }
                append(")")
            }
        } ?: ""

        val sql = buildString {
            append("CREATE TABLE IF NOT EXISTS ")
            append(tr.identity(this@YdbIdTable))
            append(" (")
            append(columnsSql)

            if (indexesSql.isNotEmpty()) {
                append(", ")
                append(indexesSql)
            }

            append(", PRIMARY KEY (")
            append(pkSql)
            append("))")
            append(ttlSql)
        }

        return listOf(sql)
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
}
