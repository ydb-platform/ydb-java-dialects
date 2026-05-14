package tech.ydb.exposed.dialect.unit.types

import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcPreparedStatementImpl
import tech.ydb.jdbc.YdbPreparedStatement
import tech.ydb.table.values.Type
import java.lang.reflect.Proxy
import java.sql.PreparedStatement

data class BoundTypedObject(
    val index: Int,
    val value: Any?,
    val type: Type
)

fun ydbPreparedStatementCapture(): Pair<JdbcPreparedStatementImpl, () -> BoundTypedObject?> {
    var boundValue: BoundTypedObject? = null

    val proxy = Proxy.newProxyInstance(
        YdbPreparedStatement::class.java.classLoader,
        arrayOf(PreparedStatement::class.java, YdbPreparedStatement::class.java)
    ) { _, method, args ->
        when (method.name) {
            "setObject" -> {
                if (args?.size == 3 && args[0] is Int && args[2] is Type) {
                    boundValue = BoundTypedObject(
                        index = args[0] as Int,
                        value = args[1],
                        type = args[2] as Type
                    )
                }
                null
            }

            "toString" -> "YdbPreparedStatementProxy"
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
