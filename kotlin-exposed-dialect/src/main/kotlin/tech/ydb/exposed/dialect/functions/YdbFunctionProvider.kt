package tech.ydb.exposed.dialect.functions

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryAlias
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.MergeStatement
import org.jetbrains.exposed.v1.core.vendors.FunctionProvider

class YdbFunctionProvider : FunctionProvider() {

    private companion object {
        const val MERGE_UNSUPPORTED =
            "YDB dialect does not support ANSI MERGE through Exposed. Use UPSERT or batchUpsert instead."
    }

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
        require(where == null) {
            "YDB UPSERT does not support WHERE clause in this dialect implementation"
        }

        val columnList = data.joinToString(", ") { (column, _) ->
            transaction.identity(column)
        }

        if (expression.isNotBlank()) {
            val valuesExpression = expression.trim()
            val expressionWithColumns = if (valuesExpression.startsWith("VALUES", ignoreCase = true)) {
                "($columnList) $valuesExpression"
            } else {
                valuesExpression
            }

            return "UPSERT INTO ${transaction.identity(table)} $expressionWithColumns"
        }

        val valueList = data.joinToString(", ") { (column, value) ->
            valueToSqlLiteral(column, value)
        }

        return "UPSERT INTO ${transaction.identity(table)} ($columnList) VALUES ($valueList)"
    }

    override fun merge(
        dest: Table,
        source: Table,
        transaction: Transaction,
        clauses: List<MergeStatement.Clause>,
        on: Op<Boolean>?
    ): String = throw UnsupportedOperationException(MERGE_UNSUPPORTED)

    override fun mergeSelect(
        dest: Table,
        source: QueryAlias,
        transaction: Transaction,
        clauses: List<MergeStatement.Clause>,
        on: Op<Boolean>,
        prepared: Boolean
    ): String = throw UnsupportedOperationException(MERGE_UNSUPPORTED)

    override fun queryLimitAndOffset(
        size: Int?,
        offset: Long,
        alreadyOrdered: Boolean
    ): String = buildString {
        if (size != null) {
            append(" LIMIT ")
            append(size)
        }
        if (offset > 0) {
            append(" OFFSET ")
            append(offset)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun valueToSqlLiteral(column: Column<*>, value: Any?): String {
        if (value == null) return "NULL"

        val columnType = column.columnType as IColumnType<Any?>
        return columnType.valueToString(value)
    }
}
