package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.basic.YdbTable
import tech.ydb.exposed.dialect.integration.base.BaseYdbTest
import tech.ydb.exposed.dialect.types.ydbDecimal
import java.math.BigDecimal

class DecimalTypesIT : BaseYdbTest() {

    object DecimalTypes : YdbTable("decimal_types") {
        val id = integer("id")
        val amount = ydbDecimal("amount", 10, 2)

        override val primaryKey = PrimaryKey(id)
    }

    override val tables: List<Table> = listOf(DecimalTypes)

    @Test
    fun `should round-trip decimal type`() = tx {
        DecimalTypes.insert {
            it[id] = 1
            it[amount] = BigDecimal("123.45")
        }

        val row = DecimalTypes.selectAll().single()
        assertEquals(BigDecimal("123.45"), row[DecimalTypes.amount].setScale(2))
    }

    @Test
    fun `should generate ddl for decimal type`() = tx {
        val ddl = DecimalTypes.ddl.joinToString(" ")
        assertTrue(ddl.contains("amount Decimal(10, 2)"))
    }
}