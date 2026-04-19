package tech.ydb.exposed.dialect.demo

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import tech.ydb.exposed.dialect.basic.YdbIndexScope
import tech.ydb.exposed.dialect.basic.YdbIndexSyncMode
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.types.ydbDecimal

object DemoProducts : YdbTable("demo_products") {
    val id = integer("id")
    val sku = varchar("sku", 64)
    val name = varchar("name", 255)
    val category = varchar("category", 128)
    val price = ydbDecimal("price", 10, 2)

    override val primaryKey = PrimaryKey(id)

    init {
        // Обычный Exposed index
        index(false, sku)

        // YDB-specific secondary index
        secondaryIndex(
            name = "demo_products_category_idx",
            category,
            unique = false,
            scope = YdbIndexScope.GLOBAL,
            syncMode = YdbIndexSyncMode.ASYNC,
            coverColumns = listOf(name, price)
        )
    }
}

fun <T : Comparable<T>> Query.keysetPage(
    column: org.jetbrains.exposed.v1.core.Column<T>,
    lastValue: T?,
    limit: Int
): Query {
    if (lastValue != null) {
        andWhere { column greater lastValue }
    }

    return orderBy(column to SortOrder.ASC).limit(limit)
}