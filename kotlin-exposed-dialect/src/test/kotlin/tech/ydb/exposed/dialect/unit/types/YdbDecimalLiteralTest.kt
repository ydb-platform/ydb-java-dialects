package tech.ydb.exposed.dialect.unit.types

import org.jetbrains.exposed.v1.core.QueryBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.types.YdbDecimalLiteral
import java.math.BigDecimal

class YdbDecimalLiteralTest {

    @Test
    fun `should render decimal literal`() {
        val expression = YdbDecimalLiteral(
            value = BigDecimal("45.00"),
            precision = 10,
            scale = 2
        )

        val queryBuilder = QueryBuilder(false)
        expression.toQueryBuilder(queryBuilder)

        assertEquals("""Decimal("45.00", 10, 2)""", queryBuilder.toString())
    }

    @Test
    fun `should reject decimal literal with scale greater than allowed`() {
        val expression = YdbDecimalLiteral(
            value = BigDecimal("45.123"),
            precision = 10,
            scale = 2
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            val queryBuilder = QueryBuilder(false)
            expression.toQueryBuilder(queryBuilder)
        }

        assertEquals(
            "Decimal value 45.123 has scale 3, which exceeds the allowed scale 2",
            error.message
        )
    }
}
