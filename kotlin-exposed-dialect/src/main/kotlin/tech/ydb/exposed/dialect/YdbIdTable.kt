package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.dao.id.IdTable

/**
 * Base class for YDB tables with a typed entity id, used with Exposed DAO.
 *
 * Same DDL surface as [YdbTable] — [ttl] / [secondaryIndex] come from [YdbTableDsl] via
 * Kotlin delegation. Subclasses declare the primary key explicitly via `final override val id`
 * / `primaryKey` (see [YdbUuidIdTable], [YdbUlidTable], [YdbStringIdTable]).
 */
abstract class YdbIdTable<T : Any>(name: String = "") :
    IdTable<T>(name),
    YdbTableDsl by YdbTableFeatures() {

    override fun createStatement(): List<String> =
        buildYdbCreateStatement(this, ttlSettings, ydbSecondaryIndices)
}
