package tech.ydb.exposed.dialect.spring.boot.autoconfigure

import org.jetbrains.exposed.v1.core.DatabaseConfig
import tech.ydb.exposed.dialect.YdbDialect
import java.sql.Connection

internal const val STARTER_YDB_JDBC_URL_PREFIX = "jdbc:ydb:"
internal const val STARTER_YDB_DRIVER_CLASS = "tech.ydb.jdbc.YdbDriver"

internal fun ydbStarterJdbcUrl(
    url: String,
    enableSignedDatetimes: Boolean = false
): String {
    val trimmedUrl = url.trim()
    val flag = "forceSignedDatetimes=$enableSignedDatetimes"

    val hashIndex = trimmedUrl.indexOf('#')
    val baseUrl = if (hashIndex >= 0) trimmedUrl.substring(0, hashIndex) else trimmedUrl
    val fragment = if (hashIndex >= 0) trimmedUrl.substring(hashIndex) else ""

    val queryIndex = baseUrl.indexOf('?')
    val path = if (queryIndex >= 0) baseUrl.substring(0, queryIndex) else baseUrl
    val query = if (queryIndex >= 0) baseUrl.substring(queryIndex + 1) else ""

    val params = query
        .split('&')
        .filter { it.isNotBlank() }
        .filterNot { it.substringBefore('=') == "forceSignedDatetimes" }
        .toMutableList()

    params += flag

    return buildString {
        append(path)
        append('?')
        append(params.joinToString("&"))
        append(fragment)
    }
}

internal fun ydbStarterDatabaseConfig(
    enableSignedDatetimes: Boolean = false
): DatabaseConfig = DatabaseConfig {
    explicitDialect = instantiateYdbDialect(enableSignedDatetimes)
    defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    defaultReadOnly = false
    useNestedTransactions = false
}

private fun instantiateYdbDialect(enableSignedDatetimes: Boolean): YdbDialect {
    val ctor = YdbDialect::class.java.getDeclaredConstructor(Boolean::class.javaPrimitiveType)
    ctor.isAccessible = true
    return ctor.newInstance(enableSignedDatetimes)
}
