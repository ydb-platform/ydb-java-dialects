package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.time.Duration

/**
 * Base class for YDB row-oriented tables.
 *
 * Adds YDB-specific DDL on top of Exposed [Table]:
 *  - [ttl] — TTL on a date/numeric column;
 *  - [secondaryIndex] — YDB secondary index with COVER / ASYNC / WITH params.
 */
open class YdbTable(name: String = "") : Table(name) {
    private var ttlSettings: YdbTtlSettings? = null
    private val secondaryIndices = mutableListOf<YdbSecondaryIndexSpec>()

    /**
     * Declares row TTL on [column] (embedded in `CREATE TABLE ... WITH (TTL = ...)`).
     *
     * @param intervalIso8601 ISO-8601 duration (e.g. `P30D`, `PT1H`).
     * @param mode How [column] is interpreted — date/timestamp types vs numeric epoch units.
     */
    fun ttl(
        column: Column<*>,
        intervalIso8601: String,
        mode: YdbTtlColumnMode = YdbTtlColumnMode.DATE_TYPE
    ) {
        ttlSettings = YdbTtlSettings(column, normalizeTtlInterval(intervalIso8601), mode)
    }

    /**
     * Declares a YDB secondary index inline in `CREATE TABLE` (not Exposed's generic [Index] DSL).
     *
     * @param scope Currently only [YdbIndexScope.GLOBAL] is supported for row tables.
     * @param syncMode [YdbIndexSyncMode.ASYNC] for background index build.
     * @param indexType Optional `USING` clause (e.g. vector index type when supported by YDB).
     * @param coverColumns Included columns for covering index (`COVER (...)`).
     * @param withParams Index-level `WITH (key = value)` parameters.
     */
    fun secondaryIndex(
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

        secondaryIndices += YdbSecondaryIndexSpec(
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

        val indexesSql = secondaryIndices.joinToString(", ") { renderYdbSecondaryIndex(it) }
        val pkSql = pk.columns.joinToString(", ") { tr.identity(it) }

        val ttlSql = ttlSettings?.let { ttl ->
            validateTtlColumn(ttl)
            buildString {
                append(" WITH (TTL = Interval(\"")
                append(escapeYqlDoubleQuotedLiteral(ttl.intervalIso8601))
                append("\") ON ")
                append(tr.identity(ttl.column))
                ttl.mode.toSql()?.let {
                    append(" AS ")
                    append(it)
                }
                append(")")
            }
        }.orEmpty()

        val sql = buildString {
            append("CREATE TABLE IF NOT EXISTS ")
            append(tr.identity(this@YdbTable))
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

    companion object {
        private fun normalizeTtlInterval(intervalIso8601: String): String =
            runCatching { Duration.parse(intervalIso8601).toString() }
                .getOrElse { cause ->
                    throw IllegalArgumentException("Invalid YDB TTL interval: '$intervalIso8601'", cause)
                }

        private fun validateTtlColumn(ttl: YdbTtlSettings) {
            val sqlType = ttl.column.columnType.sqlType()

            val supported = when (ttl.mode) {
                YdbTtlColumnMode.DATE_TYPE ->
                    sqlType == "Date" ||
                            sqlType == "Date32" ||
                            sqlType == "Datetime" ||
                            sqlType == "Datetime64" ||
                            sqlType == "Timestamp" ||
                            sqlType == "Timestamp64"

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

        private fun escapeYqlDoubleQuotedLiteral(value: String): String =
            value.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
