package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import tech.ydb.exposed.dialect.YdbIndexScope
import tech.ydb.exposed.dialect.YdbIndexSyncMode
import tech.ydb.exposed.dialect.YdbSecondaryIndexSpec
import tech.ydb.exposed.dialect.YdbTableFeatures
import tech.ydb.exposed.dialect.YdbTtlColumnMode
import tech.ydb.exposed.dialect.YdbTtlSettings
import tech.ydb.exposed.dialect.buildYdbCreateStatement

abstract class YdbIdTable<T : Any>(name: String = "") : IdTable<T>(name) {
    private val ydbFeatures = YdbTableFeatures()

    protected fun ttl(
        column: Column<*>,
        intervalIso8601: String,
        mode: YdbTtlColumnMode = YdbTtlColumnMode.DATE_TYPE
    ) {
        ydbFeatures.ttl(column, intervalIso8601, mode)
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
        ydbFeatures.secondaryIndex(
            name = name,
            columns = *columns,
            unique = unique,
            scope = scope,
            syncMode = syncMode,
            indexType = indexType,
            coverColumns = coverColumns,
            withParams = withParams
        )
    }

    val ttlSettings: YdbTtlSettings?
        get() = ydbFeatures.ttlSettings

    val ydbSecondaryIndices: List<YdbSecondaryIndexSpec>
        get() = ydbFeatures.ydbSecondaryIndices

    override fun createStatement(): List<String> =
        buildYdbCreateStatement(this, ttlSettings, ydbSecondaryIndices)
}