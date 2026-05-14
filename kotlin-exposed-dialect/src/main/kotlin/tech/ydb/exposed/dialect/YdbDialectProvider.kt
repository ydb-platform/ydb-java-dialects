package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.Connection
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Entry point that wires the YDB dialect into Exposed and produces a [Database] tuned for YDB.
 *
 * Registration with Exposed is performed once per JVM in [init], which is automatically called
 * by [connect]. After that, plain `Database.connect("jdbc:ydb:...")` also works because Exposed
 * resolves dialect/driver via the registered prefix.
 */
object YdbDialectProvider {
    private const val URL_PREFIX = "jdbc:ydb:"
    private const val DRIVER_CLASS = "tech.ydb.jdbc.YdbDriver"
    internal const val FORCE_SIGNED_DATETIMES_PROPERTY = "forceSignedDatetimes"

    private val initialized = AtomicBoolean(false)

    /**
     * Registers the YDB JDBC driver, dialect name and dialect metadata with Exposed.
     * Idempotent — repeated calls are no-ops.
     */
    fun init() {
        if (!initialized.compareAndSet(false, true)) return

        Database.registerJdbcDriver(
            prefix = URL_PREFIX,
            driverClassName = DRIVER_CLASS,
            dialect = YdbDialect.DIALECT_NAME
        )

        Database.registerDialectMetadata(YdbDialect.DIALECT_NAME) {
            YdbDialect.Metadata
        }
    }

    /**
     * Opens a YDB-backed Exposed [Database].
     *
     * @param url JDBC URL, e.g. `jdbc:ydb:grpc://localhost:2136/local`.
     * @param forceLegacyDatetimes When `true`, the dialect emits legacy YDB temporal types
     * (`Date`, `Datetime`, `Timestamp`) instead of the default extended ones
     * (`Date32`, `Datetime64`, `Timestamp64`). The same mode is also propagated to the YDB JDBC
     * driver via the `forceSignedDatetimes` URL flag, so column DDL and value binding stay in sync.
     * Use legacy mode only when integrating with schemas that already rely on the unsigned range.
     */
    fun connect(
        url: String,
        user: String = "",
        password: String = "",
        forceLegacyDatetimes: Boolean = false
    ): Database {
        init()
        val driverUrl = withTemporalDriverMode(url, forceLegacyDatetimes)

        return Database.connect(
            url = driverUrl,
            driver = DRIVER_CLASS,
            user = user,
            password = password,
            databaseConfig = DatabaseConfig {
                explicitDialect = YdbDialect(forceLegacyDatetimes = forceLegacyDatetimes)
                defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                defaultReadOnly = false
                useNestedTransactions = false
            }
        )
    }

    internal fun withTemporalDriverMode(url: String, forceLegacyDatetimes: Boolean): String =
        withBooleanQueryParameter(
            url = url,
            name = FORCE_SIGNED_DATETIMES_PROPERTY,
            value = !forceLegacyDatetimes
        )

    private fun withBooleanQueryParameter(url: String, name: String, value: Boolean): String {
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
                val key = param.substringBefore('=')
                key == name
            }
            .toMutableList()

        filteredParams += "$name=$value"
        return "$base?${filteredParams.joinToString("&")}"
    }
}
