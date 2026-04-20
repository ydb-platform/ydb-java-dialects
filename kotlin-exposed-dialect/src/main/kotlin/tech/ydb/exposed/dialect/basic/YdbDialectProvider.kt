package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.Connection

object YdbDialectProvider {

    private const val DEFAULT_DRIVER = "tech.ydb.jdbc.YdbDriver"

    fun connect(
        url: String,
        driver: String = DEFAULT_DRIVER,
        user: String = "",
        password: String = ""
    ): Database {
        YdbExposedBootstrap.init()

        return Database.connect(
            url = url,
            driver = driver,
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