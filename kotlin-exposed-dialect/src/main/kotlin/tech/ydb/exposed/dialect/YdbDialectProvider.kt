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
     * (`Date32`, `Datetime64`, `Timestamp64`). Use this only when integrating with schemas that
     * already rely on the unsigned/legacy range.
     */
    fun connect(
        url: String,
        user: String = "",
        password: String = "",
        forceLegacyDatetimes: Boolean = false
    ): Database {
        init()

        return Database.connect(
            url = url,
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
}
