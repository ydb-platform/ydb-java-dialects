package tech.ydb.exposed.dialect.types

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.QueryBuilder
import java.math.BigDecimal

class YdbDecimalLiteral(
    private val value: BigDecimal,
    private val precision: Int,
    private val scale: Int
) : Expression<BigDecimal>() {

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        val normalized = value.setScale(scale).toPlainString()
        queryBuilder.append("""Decimal("$normalized", $precision, $scale)""")
    }
}

fun ydbDecimalLiteral(
    value: BigDecimal,
    precision: Int,
    scale: Int
): Expression<BigDecimal> = YdbDecimalLiteral(value, precision, scale)