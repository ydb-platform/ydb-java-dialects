package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager


open class YdbTable(name: String = "") : Table(name) {

    override fun createStatement(): List<String> {
        val tr = TransactionManager.current()

        val pk = primaryKey
            ?: error("YDB requires PRIMARY KEY for every table: $tableName")

        val columnsSql = columns.joinToString(", ") { column ->
            buildString {
                append(tr.identity(column))
                append(" ")
                append(column.columnType.sqlType())

                if (!column.columnType.nullable) {
                    append(" NOT NULL")
                }

                // Пока сознательно пропускаем DEFAULT, UNIQUE, FK, CHECK
                // и любые inline constraints.
            }
        }

        val pkSql = pk.columns.joinToString(", ") { tr.identity(it) }

        val sql = buildString {
            append("CREATE TABLE IF NOT EXISTS ")
            append(tr.identity(this@YdbTable))
            append(" (")
            append(columnsSql)
            append(", PRIMARY KEY (")
            append(pkSql)
            append("))")
        }

        return listOf(sql)
    }
}