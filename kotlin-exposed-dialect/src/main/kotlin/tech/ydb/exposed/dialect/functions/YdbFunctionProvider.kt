package tech.ydb.exposed.dialect.functions

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.vendors.FunctionProvider

class YdbFunctionProvider : FunctionProvider() {

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
        require(onUpdate.isEmpty()) {
            "YDB UPSERT does not use ON UPDATE clause in this dialect implementation"
        }

        val columnList = data.joinToString(", ") { (column, _) ->
            transaction.identity(column)
        }

        val valueList = data.joinToString(", ") { (column, value) ->
            valueToSqlLiteral(column, value)
        }

        return buildString {
            append("UPSERT INTO ")
            append(transaction.identity(table))
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
            if (size != null) {
                append(" LIMIT ")
                append(size)
            }
            if (offset > 0) {
                append(" OFFSET ")
                append(offset)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun valueToSqlLiteral(column: Column<*>, value: Any?): String {
        if (value == null) return "NULL"

        val columnType = column.columnType as org.jetbrains.exposed.v1.core.IColumnType<Any?>
        return columnType.valueToString(value)
    }
}

//package tech.ydb.exposed.dialect.functions
//
//import org.jetbrains.exposed.v1.core.Column
//import org.jetbrains.exposed.v1.core.Op
//import org.jetbrains.exposed.v1.core.Table
//import org.jetbrains.exposed.v1.core.Transaction
//import org.jetbrains.exposed.v1.core.vendors.FunctionProvider
//
//class YdbFunctionProvider: FunctionProvider() {
//    override fun random(seed: Int?): String = "Random()"
//
//    override fun upsert(
//        table: Table,
//        data: List<Pair<Column<*>, Any?>>,
//        expression: String,
//        onUpdate: List<Pair<Column<*>, Any?>>,
//        keyColumns: List<Column<*>>,
//        where: Op<Boolean>?,
//        transaction: Transaction
//    ): String {
//        val columns = data.map { it.first.name }
//
//        val values = data.map { (_, value) ->
//            when (value) {
//                null -> "NULL"
//                is String -> "'$value'"
//                else -> value.toString()
//            }
//        }
//
//        val columnList = columns.joinToString(", ")
//        val valueList = values.joinToString(", ")
//
//        return buildString {
//            append("UPSERT INTO ")
//            append(table.tableName)
//            append(" (")
//            append(columnList)
//            append(") VALUES (")
//            append(valueList)
//            append(")")
//        }
//    }
//
//    override fun queryLimitAndOffset(
//        size: Int?,
//        offset: Long,
//        alreadyOrdered: Boolean
//    ): String {
//        return buildString {
//            append(" LIMIT ")
//            append(size)
//
//            if (offset > 0) {
//                append(" OFFSET ")
//                append(offset)
//            }
//        }
//    }
//}