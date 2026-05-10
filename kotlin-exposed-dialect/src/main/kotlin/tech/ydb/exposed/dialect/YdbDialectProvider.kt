package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.Connection
import java.util.concurrent.atomic.AtomicBoolean

object YdbDialectProvider {
    private const val DIALECT_NAME = "ydb"
    private const val URL_PREFIX = "jdbc:ydb:"
    private const val DRIVER_CLASS = "tech.ydb.jdbc.YdbDriver"

    private val initialized = AtomicBoolean(false)

    fun init() {
        if (!initialized.compareAndSet(false, true)) return

        Database.registerJdbcDriver(
            prefix = URL_PREFIX,
            driverClassName = DRIVER_CLASS,
            dialect = DIALECT_NAME
        )

        Database.registerDialectMetadata(DIALECT_NAME) {
            YdbDialect.Metadata
        }
    }

    fun connect(
        url: String,
        user: String = "",
        password: String = ""
    ): Database {
        init()

        return Database.connect(
            url = url,
            driver = DRIVER_CLASS,
            user = user,
            password = password,
            databaseConfig = DatabaseConfig {
                explicitDialect = YdbDialect()
                defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                defaultReadOnly = false
                useNestedTransactions = false
            }
        )
    }
}
