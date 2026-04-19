package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.types.ydbDecimal
import tech.ydb.exposed.dialect.types.ydbDecimalLiteral
import java.math.BigDecimal

class DecimalUpdateIT : BaseYdbTest() {

    object DecimalItems : YdbTable("decimal_update_items") {
        val id = integer("id")
        val name = varchar("name", 255)
        val price = ydbDecimal("price", 10, 2)

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(DecimalItems)

    @Test
    fun `should update decimal value using ydb decimal literal`() = tx {
        DecimalItems.insert {
            it[id] = 1
            it[name] = "book"
            it[price] = BigDecimal("39.90")
        }

        DecimalItems.update({ DecimalItems.id eq 1 }) {
            it.update(
                DecimalItems.price,
                ydbDecimalLiteral(BigDecimal("45.00"), 10, 2)
            )
        }

        val row = DecimalItems.selectAll().single()

        assertEquals(BigDecimal("45.00"), row[DecimalItems.price])
    }

    @Test
    fun `should update decimal and another column in one statement`() = tx {
        DecimalItems.insert {
            it[id] = 2
            it[name] = "draft"
            it[price] = BigDecimal("10.50")
        }

        DecimalItems.update({ DecimalItems.id eq 2 }) {
            it[name] = "published"
            it.update(
                DecimalItems.price,
                ydbDecimalLiteral(BigDecimal("12.75"), 10, 2)
            )
        }

        val row = DecimalItems.selectAll().where { DecimalItems.id eq 2 }.single()

        assertEquals("published", row[DecimalItems.name])
        assertEquals(BigDecimal("12.75"), row[DecimalItems.price])
    }
}