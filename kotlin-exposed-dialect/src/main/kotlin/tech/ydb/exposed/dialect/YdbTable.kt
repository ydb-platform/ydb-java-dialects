package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

/**
 * Base class for YDB row-oriented tables.
 *
 * Adds YDB-specific DDL on top of Exposed [Table]:
 *  - [ttl] — TTL on a date/numeric column;
 *  - [secondaryIndex] — YDB secondary index with COVER / ASYNC / WITH params.
 */
open class YdbTable(name: String = "") : Table(name) {

    private var ttlSettingsState: YdbTtlSettings? = null
    private val secondaryIndices = mutableListOf<YdbSecondaryIndexSpec>()

    /**
     * Declares row TTL on [column] (embedded in `CREATE TABLE ... WITH (TTL = ...)`).
     *
     * @param intervalIso8601 ISO-8601 duration (e.g. `P30D`, `PT1H`); normalized via [normalizeTtlInterval].
     * @param mode How [column] is interpreted — date/timestamp types vs numeric epoch units.
     */
    fun ttl(
        column: Column<*>,
        intervalIso8601: String,
        mode: YdbTtlColumnMode = YdbTtlColumnMode.DATE_TYPE
    ) {
        ttlSettingsState = YdbTtlSettings(column, normalizeTtlInterval(intervalIso8601), mode)
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

    internal val ttlSettings: YdbTtlSettings?
        get() = ttlSettingsState

    private val ydbSecondaryIndices: List<YdbSecondaryIndexSpec>
        get() = secondaryIndices

    override fun createStatement(): List<String> =
        buildYdbCreateStatement(this, ttlSettings, ydbSecondaryIndices)
}
