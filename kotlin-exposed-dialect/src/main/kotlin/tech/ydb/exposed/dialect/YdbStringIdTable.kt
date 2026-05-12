package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID

/**
 * IdTable with a user-provided string primary key (e.g. business keys, slugs).
 *
 * No client default — callers must set `[idColumn] = "..."` on every insert.
 * For auto-generated ids prefer [YdbUuidIdTable] (native UUID) or [YdbUlidTable] (sortable).
 */
open class YdbStringIdTable(
    name: String = "",
    idLength: Int = 64
) : YdbIdTable<String>(name) {

    final override val id: Column<EntityID<String>> = varchar("id", idLength).entityId()

    final override val primaryKey = PrimaryKey(id)
}
