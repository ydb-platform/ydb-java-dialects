package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.jdbc.Database
import java.util.concurrent.atomic.AtomicBoolean

object YdbExposedBootstrap {
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
            YdbDialectMetadata()
        }

    }
}