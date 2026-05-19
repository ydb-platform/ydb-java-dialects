package tech.ydb.exposed.dialect.example

import org.jetbrains.exposed.v1.core.Table
import tech.ydb.exposed.dialect.ydbDecimal

object DemoProducts : Table("demo_products") {
    val id = integer("id")
    val sku = varchar("sku", 64)
    val name = varchar("name", 255)
    val category = varchar("category", 128)
    val price = ydbDecimal("price", 10, 2)

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, sku)
        index("demo_products_category_idx", isUnique = false, category)
    }
}
