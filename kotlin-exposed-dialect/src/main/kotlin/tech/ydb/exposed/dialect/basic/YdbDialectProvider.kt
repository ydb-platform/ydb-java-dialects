package tech.ydb.exposed.dialect.basic

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

object YdbDialectProvider {

    private const val DEFAULT_DRIVER = "tech.ydb.jdbc.YdbDriver"

    fun connect(
        url: String,
        driver: String = DEFAULT_DRIVER,
        user: String = "",
        password: String = ""
    ): Database {
        Class.forName(driver).getDeclaredConstructor().newInstance()

        val props = Properties().apply {
            if (user.isNotEmpty()) setProperty("user", user)
            if (password.isNotEmpty()) setProperty("password", password)
        }

        return Database.connect(
            getNewConnection = {
                DriverManager.getConnection(url, props)
            },
            databaseConfig = DatabaseConfig {
                explicitDialect = YdbDialect()

                // Для YDB нельзя оставлять дефолт Exposed/JDBC,
                // иначе transaction() попытается выставить READ_COMMITTED (2).
                defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

                // Пока безопаснее явно зафиксировать.
                defaultReadOnly = false

                // Опционально: без nested transactions на первом этапе.
                useNestedTransactions = false
            }
        )
    }
}

//package tech.ydb.exposed.dialect.basic
//
//import org.jetbrains.exposed.v1.core.DatabaseConfig
//import org.jetbrains.exposed.v1.jdbc.Database
//
//object YdbDialectProvider {
//
//    fun connect(
//        url: String,
//        driver: String,
//        user: String = "",
//        password: String = ""
//    ): Database {
//        return Database.connect(
//            url = url,
//            driver = driver,
//            user = user,
//            password = password,
//            databaseConfig = DatabaseConfig {
//                explicitDialect = YdbDialect()
//            }
//        )
//    }
//}

////object YdbDialectProvider {
////
////    fun connect(
////        url: String = "jdbc:ydb:grpc://localhost:2136/local",
////        driver: String = "tech.ydb.jdbc.YdbDriver",
////        user: String = "",
////        password: String = ""
////    ): Database {
////
////        val config = DatabaseConfig.Companion {
////            defaultFetchSize = 1000
////        }
////
////        return Database.connect(
////            url = url,
////            driver = driver,
////            user = user,
////            password = password,
////            databaseConfig = config,
////            dialect = YdbDialect(true),
////            setupConnection = TODO(),
////            connectionAutoRegistration = TODO(),
////            manager = TODO()
////        )
////    }
////}