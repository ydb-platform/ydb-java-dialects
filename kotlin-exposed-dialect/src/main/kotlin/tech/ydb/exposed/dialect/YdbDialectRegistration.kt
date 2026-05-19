/**
 * Registers the YDB JDBC driver and Exposed [YdbDialect].
 *
 * After [registerYdbDialect], open a database with `Database.connect("jdbc:ydb:...")`
 * and use [ydbTransaction] for DML under YDB OCC.
 */
package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.DatabaseApi
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.Connection

const val YDB_JDBC_URL_PREFIX = "jdbc:ydb:"

const val YDB_DRIVER_CLASS = "tech.ydb.jdbc.YdbDriver"

const val FORCE_SIGNED_DATETIMES_PROPERTY = "forceSignedDatetimes"

/**
 * Registers the YDB JDBC driver and Exposed dialect.
 *
 * @param enableSignedDatetimes When `true`, standard Exposed `date` / `datetime` / `timestamp` DDL
 *   uses `Date32` / `Datetime64` / `Timestamp64`. Per-column types stay explicit via `javatime.*`.
 */
fun registerYdbDialect(enableSignedDatetimes: Boolean = false) {
    Database.registerJdbcDriver(
        prefix = YDB_JDBC_URL_PREFIX,
        driverClassName = YDB_DRIVER_CLASS,
        dialect = YdbDialect.DIALECT_NAME
    )

    Database.registerDialectMetadata(YdbDialect.DIALECT_NAME) {
        YdbDialectMetadata
    }

    DatabaseApi.registerDialect(YdbDialect.DIALECT_NAME) {
        YdbDialect(enableSignedDatetimes)
    }
}

/**
 * Recommended Exposed [DatabaseConfig] for YDB-backed JDBC databases.
 *
 * This helper is intentionally Spring-neutral so integrations outside of [registerYdbDialect]
 * can reuse the same YDB defaults and explicit dialect selection.
 */
fun ydbDatabaseConfig(enableSignedDatetimes: Boolean = false): DatabaseConfig = DatabaseConfig {
    explicitDialect = YdbDialect(enableSignedDatetimes = enableSignedDatetimes)
    defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    defaultReadOnly = false
    useNestedTransactions = false
}

/**
 * Ensures that a YDB JDBC URL contains a `forceSignedDatetimes` flag aligned with the selected
 * temporal mode.
 */
fun ydbJdbcUrl(
    url: String,
    enableSignedDatetimes: Boolean = false
): String =
    appendBooleanQueryParameter(
        url = url,
        name = FORCE_SIGNED_DATETIMES_PROPERTY,
        value = enableSignedDatetimes
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
