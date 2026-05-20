package tech.ydb.exposed.dialect.unit.types

import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcPreparedStatementImpl
import java.lang.reflect.Proxy
import java.sql.PreparedStatement

data class BoundSqlObject(
    val index: Int,
    val value: Any?,
    val targetSqlType: Int
)

fun ydbPreparedStatementCapture(): Pair<JdbcPreparedStatementImpl, () -> BoundSqlObject?> {
    var boundValue: BoundSqlObject? = null

    val proxy = Proxy.newProxyInstance(
        PreparedStatement::class.java.classLoader,
        arrayOf(PreparedStatement::class.java)
    ) { _, method, args ->
        when (method.name) {
            "setObject" -> {
                if (args?.size == 3 && args[0] is Int && args[2] is Int) {
                    boundValue = BoundSqlObject(
                        index = args[0] as Int,
                        value = args[1],
                        targetSqlType = args[2] as Int
                    )
                }
                null
            }

            "toString" -> "PreparedStatementProxy"
            "hashCode" -> 0
            "equals" -> false
            "isClosed" -> false
            "execute" -> false
            "executeUpdate" -> 0
            "executeLargeUpdate" -> 0L
            "getUpdateCount" -> 0
            "getLargeUpdateCount" -> 0L
            "getMoreResults" -> false
            else -> defaultValue(method.returnType)
        }
    } as PreparedStatement

    return JdbcPreparedStatementImpl(proxy, false) to { boundValue }
}

private fun defaultValue(type: Class<*>): Any? = when (type) {
    java.lang.Boolean.TYPE -> false
    java.lang.Integer.TYPE -> 0
    java.lang.Long.TYPE -> 0L
    java.lang.Short.TYPE -> 0.toShort()
    java.lang.Byte.TYPE -> 0.toByte()
    java.lang.Float.TYPE -> 0f
    java.lang.Double.TYPE -> 0.0
    java.lang.Character.TYPE -> '\u0000'
    else -> null
}
