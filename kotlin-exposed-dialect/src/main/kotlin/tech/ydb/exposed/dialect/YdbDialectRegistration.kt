package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.DatabaseApi
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.Connection
import java.util.concurrent.atomic.AtomicBoolean

internal const val YDB_JDBC_URL_PREFIX = "jdbc:ydb:"
internal const val YDB_DRIVER_CLASS = "tech.ydb.jdbc.YdbDriver"
internal const val FORCE_SIGNED_DATETIMES_PROPERTY = "forceSignedDatetimes"

private val dialectRegistered = AtomicBoolean(false)

/**
 * Registers the YDB JDBC driver and Exposed dialect (idempotent).
 *
 * After registration, `Database.connect("jdbc:ydb:...")` resolves [YdbDialect] automatically.
 */
fun registerYdbDialect() {
    if (!dialectRegistered.compareAndSet(false, true)) {
        return
    }

    Database.registerJdbcDriver(
        prefix = YDB_JDBC_URL_PREFIX,
        driverClassName = YDB_DRIVER_CLASS,
        dialect = YdbDialect.DIALECT_NAME
    )

    Database.registerDialectMetadata(YdbDialect.DIALECT_NAME) {
        YdbDialect.Metadata
    }

    DatabaseApi.registerDialect(YdbDialect.DIALECT_NAME) {
        YdbDialect()
    }
}

/**
 * Opens a YDB-backed Exposed [Database] with dialect defaults tuned for YDB.
 *
 * Add `forceSignedDatetimes=true` or `forceSignedDatetimes=false` to [url] when the JDBC driver requires it.
 */
fun connectYdb(
    url: String,
    user: String = "",
    password: String = "",
    enableSignedDatetimes: Boolean = false
): Database {
    ensureYdbDialectRegistered()

    return Database.connect(
        url = url,
        driver = YDB_DRIVER_CLASS,
        user = user,
        password = password,
        databaseConfig = ydbDatabaseConfig(enableSignedDatetimes = enableSignedDatetimes)
    )
}

internal fun ydbDatabaseConfig(enableSignedDatetimes: Boolean = false): DatabaseConfig = DatabaseConfig {
    explicitDialect = YdbDialect(enableSignedDatetimes = enableSignedDatetimes)
    defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    defaultReadOnly = false
    useNestedTransactions = false
}

internal fun ydbJdbcUrl(url: String): String =
    appendBooleanQueryParameter(
        url = url,
        name = FORCE_SIGNED_DATETIMES_PROPERTY,
        value = false
    )

private fun appendBooleanQueryParameter(url: String, name: String, value: Boolean): String {
    val queryStart = url.indexOf('?')
    if (queryStart < 0) {
        return "$url?$name=$value"
    }

    val base = url.substring(0, queryStart)
    val query = url.substring(queryStart + 1)
    val filteredParams = query
        .split('&')
        .filter { it.isNotBlank() }
        .filterNot { param ->
            param.substringBefore('=') == name
        }
        .toMutableList()

    filteredParams += "$name=$value"
    return "$base?${filteredParams.joinToString("&")}"
}

private fun ensureYdbDialectRegistered() {
    if (dialectRegistered.get()) {
        return
    }
    registerYdbDialect()
}
