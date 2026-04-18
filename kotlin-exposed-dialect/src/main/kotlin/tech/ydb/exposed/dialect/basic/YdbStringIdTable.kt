package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID


open class YdbStringIdTable(
    name: String = "",
    idLength: Int = 64
) : YdbIdTable<String>(name) {

    final override val id: Column<EntityID<String>> = varchar("id", idLength).entityId()

    final override val primaryKey = PrimaryKey(id)
}