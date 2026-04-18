package tech.ydb.exposed.dialect.pagination

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere

/**
 * Keyset pagination helper for YDB-oriented Exposed queries.
 *
 * ASC:
 *   SELECT ... WHERE key > :lastValue ORDER BY key ASC LIMIT :limit
 *
 * DESC:
 *   SELECT ... WHERE key < :lastValue ORDER BY key DESC LIMIT :limit
 *
 * Intended for sortable columns, ideally primary key columns.
 */
fun <T : Comparable<T>> Query.keysetPage(
    column: Column<T>,
    lastValue: T?,
    limit: Int,
    direction: SortOrder = SortOrder.ASC
): Query {
    require(limit > 0) { "limit must be > 0" }
    require(direction == SortOrder.ASC || direction == SortOrder.DESC) {
        "keysetPage supports only ASC and DESC sort orders"
    }

    if (lastValue != null) {
        when (direction) {
            SortOrder.ASC -> andWhere { column greater lastValue }
            SortOrder.DESC -> andWhere { column less lastValue }
            else -> error("Unsupported sort order: $direction")
        }
    }

    return orderBy(column to direction).limit(limit)
}

fun <T : Comparable<T>> Query.keysetPageAsc(
    column: Column<T>,
    lastValue: T?,
    limit: Int
): Query = keysetPage(
    column = column,
    lastValue = lastValue,
    limit = limit,
    direction = SortOrder.ASC
)

fun <T : Comparable<T>> Query.keysetPageDesc(
    column: Column<T>,
    lastValue: T?,
    limit: Int
): Query = keysetPage(
    column = column,
    lastValue = lastValue,
    limit = limit,
    direction = SortOrder.DESC
)