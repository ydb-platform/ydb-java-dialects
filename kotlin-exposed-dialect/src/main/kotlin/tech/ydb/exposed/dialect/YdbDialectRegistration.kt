/**
 * Registers the YDB JDBC driver and Exposed [YdbDialect].
 *
 * After [registerYdbDialect], open a database with `Database.connect("jdbc:ydb:...")`
 * and use [ydbTransaction] for DML under YDB OCC.
 */
package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.DatabaseApi
import org.jetbrains.exposed.v1.jdbc.Database

internal const val YDB_JDBC_URL_PREFIX = "jdbc:ydb:"

internal const val YDB_DRIVER_CLASS = "tech.ydb.jdbc.YdbDriver"

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
