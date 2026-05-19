/**
 * Entry points for wiring YDB into Exposed: driver registration, [connectYdb], and JDBC URL helpers.
 *
 * Typical setup:
 * 1. [registerYdbDialect] once (or let [connectYdb] do it).
 * 2. [connectYdb] with a `jdbc:ydb:...` URL and optional [enableSignedDatetimes].
 * 3. Declare tables as [YdbTable] and use [ydbTransaction] for DML under YDB OCC.
 */
package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.DatabaseApi
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.Connection
import java.util.concurrent.atomic.AtomicBoolean

/** JDBC URL prefix registered with Exposed ([registerYdbDialect]). */
internal const val YDB_JDBC_URL_PREFIX = "jdbc:ydb:"

/** YDB JDBC driver class ([tech.ydb.jdbc.YdbDriver]). */
internal const val YDB_DRIVER_CLASS = "tech.ydb.jdbc.YdbDriver"

/**
 * Query parameter for the YDB JDBC driver: signed vs legacy temporal wire format.
 * Not set automatically by [connectYdb] — add `forceSignedDatetimes=true|false` to the URL explicitly.
 */
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
 * @param url JDBC URL (`jdbc:ydb:grpc://...` or `grpcs://...`). Append `forceSignedDatetimes=true`
 *   or `false` when the driver requires it (not added automatically).
 * @param enableSignedDatetimes When `true`, [YdbDialect] maps standard Exposed `date` / `datetime` /
 *   `timestamp` DDL names to `Date32` / `Datetime64` / `Timestamp64`. Per-column types remain
 *   explicit via [tech.ydb.exposed.dialect.javatime.ydbDate] vs [ydbDate32], etc.
 *
 * Configures `SERIALIZABLE` isolation and disables nested transactions (YDB snapshot semantics).
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

/** [DatabaseConfig] used by [connectYdb] with an explicit [YdbDialect] instance. */
internal fun ydbDatabaseConfig(enableSignedDatetimes: Boolean = false): DatabaseConfig = DatabaseConfig {
    explicitDialect = YdbDialect(enableSignedDatetimes = enableSignedDatetimes)
    defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    defaultReadOnly = false
    useNestedTransactions = false
}

/**
 * Appends `forceSignedDatetimes=false` to [url] (replacing an existing value).
 * Used by tests and [RegisterYdbDialectConnectIT]; [connectYdb] does not modify the URL.
 */
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
