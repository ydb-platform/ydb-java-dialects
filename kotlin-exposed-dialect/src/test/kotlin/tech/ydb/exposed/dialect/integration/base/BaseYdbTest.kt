package tech.ydb.exposed.dialect.integration.base

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import tech.ydb.exposed.dialect.basic.YdbDialectProvider
import java.sql.Connection

abstract class BaseYdbTest {

    protected lateinit var db: Database

    protected open val tables: List<Table> = emptyList()

    @BeforeEach
    fun setupDatabase() {
        db = YdbDialectProvider.connect(
            url = "jdbc:ydb:grpc://localhost:2136/local",
            driver = "tech.ydb.jdbc.YdbDriver"
        )
    }

    @BeforeEach
    fun setupSchema() = transaction(
        db = db,
        transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
        readOnly = false
    ) {
        if (tables.isNotEmpty()) {
            runCatching { SchemaUtils.drop(*tables.toTypedArray()) }
            SchemaUtils.create(*tables.toTypedArray())
        }
    }

    @AfterEach
    fun teardownSchema() {
        if (!::db.isInitialized) return

        transaction(
            db = db,
            transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            readOnly = false
        ) {
            if (tables.isNotEmpty()) {
                runCatching { SchemaUtils.drop(*tables.toTypedArray()) }
                SchemaUtils.create(*tables.toTypedArray())
            }
        }

        runCatching {
            TransactionManager.closeAndUnregister(db)
        }
    }

    protected fun tx(block: JdbcTransaction.() -> Unit) =
        transaction(
            db = db,
            transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            readOnly = false
        ) {
            block()
        }
}