package tech.ydb.exposed.dialect.pagination

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere

/**
 * Helper для keyset pagination в YDB.
 *
 * Генерирует:
 * SELECT ... WHERE key > lastKey ORDER BY key LIMIT N
 */
fun <T : Comparable<T>> Query.keysetPage(
    column: Column<T>,
    lastValue: T?,
    limit: Int
): Query {

    if (lastValue != null) {
        this.andWhere { column greater lastValue }
    }

    return this
        .orderBy(column to SortOrder.ASC)
        .limit(limit)
}