package tech.ydb.exposed.dialect.functions

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.vendors.FunctionProvider

class YdbFunctionProvider: FunctionProvider() {
    override fun random(seed: Int?): String = "Random()"

    override fun upsert(
        table: Table,
        data: List<Pair<Column<*>, Any?>>,
        expression: String,
        onUpdate: List<Pair<Column<*>, Any?>>,
        keyColumns: List<Column<*>>,
        where: Op<Boolean>?,
        transaction: Transaction
    ): String {
        val columns = data.map { it.first.name }

        val values = data.map { (_, value) ->
            when (value) {
                null -> "NULL"
                is String -> "'$value'"
                else -> value.toString()
            }
        }

        val columnList = columns.joinToString(", ")
        val valueList = values.joinToString(", ")

        return buildString {
            append("UPSERT INTO ")
            append(table.tableName)
            append(" (")
            append(columnList)
            append(") VALUES (")
            append(valueList)
            append(")")
        }
    }

    override fun queryLimitAndOffset(
        size: Int?,
        offset: Long,
        alreadyOrdered: Boolean
    ): String {
        return buildString {
            append(" LIMIT ")
            append(size)

            if (offset > 0) {
                append(" OFFSET ")
                append(offset)
            }
        }
    }
}