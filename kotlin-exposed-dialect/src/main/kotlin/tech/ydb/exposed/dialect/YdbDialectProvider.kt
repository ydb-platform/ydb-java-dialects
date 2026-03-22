package tech.ydb.exposed.dialect

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database


object YdbDialectProvider {
    
    fun connect(
        url: String = "jdbc:ydb:grpc://localhost:2136/local",
        driver: String = "tech.ydb.jdbc.YdbDriver",
        user: String = "",
        password: String = ""
    ): Database {

        val config = DatabaseConfig {
            defaultFetchSize = 1000
        }

        return Database.connect(
            url = url,
            driver = driver,
            user = user,
            password = password,
            databaseConfig = config,
            dialect = YdbDialect(true),
            setupConnection = TODO(),
            connectionAutoRegistration = TODO(),
            manager = TODO()
        )
    }
}