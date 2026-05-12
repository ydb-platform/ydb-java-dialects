package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

/**
 * YDB-specific DDL surface shared by [YdbTable] and [YdbIdTable].
 *
 * Both implement this interface through Kotlin delegation to a single [YdbTableFeatures]
 * instance, so TTL/secondary-index state is collected and rendered in exactly one place.
 */
sealed interface YdbTableDsl {
    fun ttl(
        column: Column<*>,
        intervalIso8601: String,
        mode: YdbTtlColumnMode = YdbTtlColumnMode.DATE_TYPE
    )

    fun secondaryIndex(
        name: String,
        vararg columns: Column<*>,
        unique: Boolean = false,
        scope: YdbIndexScope = YdbIndexScope.GLOBAL,
        syncMode: YdbIndexSyncMode = YdbIndexSyncMode.SYNC,
        indexType: String? = null,
        coverColumns: List<Column<*>> = emptyList(),
        withParams: Map<String, Any> = emptyMap()
    )

    val ttlSettings: YdbTtlSettings?
    val ydbSecondaryIndices: List<YdbSecondaryIndexSpec>
}

/**
 * Default in-memory implementation of [YdbTableDsl] used as a delegate by [YdbTable] and
 * [YdbIdTable]. Users typically don't need to reference this class directly.
 */
class YdbTableFeatures : YdbTableDsl {
    private var ttlSettingsState: YdbTtlSettings? = null
    private val secondaryIndices = mutableListOf<YdbSecondaryIndexSpec>()

    override fun ttl(column: Column<*>, intervalIso8601: String, mode: YdbTtlColumnMode) {
        ttlSettingsState = YdbTtlSettings(column, intervalIso8601, mode)
    }

    override fun secondaryIndex(
        name: String,
        vararg columns: Column<*>,
        unique: Boolean,
        scope: YdbIndexScope,
        syncMode: YdbIndexSyncMode,
        indexType: String?,
        coverColumns: List<Column<*>>,
        withParams: Map<String, Any>
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

    override val ttlSettings: YdbTtlSettings?
        get() = ttlSettingsState

    override val ydbSecondaryIndices: List<YdbSecondaryIndexSpec>
        get() = secondaryIndices
}

internal fun buildYdbCreateStatement(
    table: Table,
    ttlSettings: YdbTtlSettings?,
    secondaryIndices: List<YdbSecondaryIndexSpec>
): List<String> {
    val tr = TransactionManager.current()

    val pk = table.primaryKey
        ?: error("YDB requires PRIMARY KEY for every table: ${table.tableName}")

    val columnsSql = table.columns.joinToString(", ") { column ->
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
        validateYdbTtlColumn(ttl)

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
    }.orEmpty()

    val sql = buildString {
        append("CREATE TABLE IF NOT EXISTS ")
        append(tr.identity(table))
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

internal fun validateYdbTtlColumn(ttl: YdbTtlSettings) {
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
