package tech.ydb.exposed.dialect.integration.base

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import tech.ydb.exposed.dialect.basic.YdbDialectProvider
import java.sql.Connection

abstract class BaseYdbTest {

    protected lateinit var db: Database

    @BeforeEach
    fun setupDatabase() {
        db = YdbDialectProvider.connect(
            url = "jdbc:ydb:grpc://localhost:2136/local",
            driver = "tech.ydb.jdbc.YdbDriver"
        )
    }

    @BeforeEach
    fun setup() {
        // Пока пусто
    }

    @AfterEach
    fun teardown() {
        if (!::db.isInitialized) return
        // Пока cleanup не делаем
    }

    protected fun tx(block: JdbcTransaction.() -> Unit) =
        transaction(
            db = db,
            transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            readOnly = false
        ) {
            block()
        }

    protected fun roTx(block: JdbcTransaction.() -> Unit) =
        transaction(
            db = db,
            transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            readOnly = true
        ) {
            block()
        }
}

//package tech.ydb.exposed.dialect.integration.base
//
//import org.jetbrains.exposed.v1.jdbc.Database
//import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
//import org.jetbrains.exposed.v1.jdbc.transactions.transaction
//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.BeforeEach
//import tech.ydb.exposed.dialect.basic.YdbDialectProvider
//
//abstract class BaseYdbTest {
//
//    protected lateinit var db: Database
//
//    @BeforeEach
//    fun setupDatabase() {
//
//        db = YdbDialectProvider.connect(
//            url = "jdbc:ydb:grpc://localhost:2136/local",
//            driver = "tech.ydb.jdbc.YdbDriver"
//        )
//    }
//
//    @BeforeEach
//    fun setup() = transaction(db) {
//        // Можно очищать схемы, если нужно
//    }
//
//    @AfterEach
//    fun teardown() = transaction(db) {
//        // Очистка таблиц после тестов
//    }
//
////    protected fun tx(block: () -> Unit) =
////        YdbTransactionManager.transactionWithRetry(db) {
////            block()
////        }
//
//    // Утилита для упрощения вызова transaction
//    protected fun tx(block: JdbcTransaction.() -> Unit) = transaction(db) { block() }
//}