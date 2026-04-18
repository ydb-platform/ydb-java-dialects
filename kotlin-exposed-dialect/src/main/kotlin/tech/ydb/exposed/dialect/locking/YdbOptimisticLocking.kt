package tech.ydb.exposed.dialect.locking

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.update

object YdbOptimisticLocking {

    fun <ID : Any> updateWithVersion(
        table: Table,
        idColumn: Column<ID>,
        idValue: ID,
        versionColumn: Column<Int>,
        expectedVersion: Int,
        body: (UpdateBuilder<*>) -> Unit
    ): Boolean {
        val updatedRows = table.update(
            where = {
                (idColumn eq idValue) and (versionColumn eq expectedVersion)
            }
        ) { stmt ->
            body(stmt)
            stmt[versionColumn] = expectedVersion + 1
        }

        return updatedRows == 1
    }
}