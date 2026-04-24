package tech.ydb.exposed.dialect.example

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
        index(false, sku)

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
