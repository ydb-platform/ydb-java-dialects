package tech.ydb.exposed.dialect.integration.base

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import tech.ydb.exposed.dialect.YdbDialectProvider
import tech.ydb.exposed.dialect.ydbTransaction
import tech.ydb.test.junit5.YdbHelperExtension

/**
 * Base class for integration tests.
 *
 * Starts a YDB testcontainer via [YdbHelperExtension] and opens an Exposed [Database]
 * against it. Schema is created in `@BeforeEach` and dropped in `@AfterEach`
 * (YDB DDL is not transactional, so we drop in teardown rather than retry-on-create).
 */
abstract class BaseYdbTest {

    protected lateinit var db: Database

    protected open val tables: List<Table> = emptyList()

    @BeforeEach
    fun setupDatabase() {
        val jdbcUrl = buildString {
            append("jdbc:ydb:")
            append(if (ydb.useTls()) "grpcs://" else "grpc://")
            append(ydb.endpoint())
            append(ydb.database())
            ydb.authToken()?.let { append("?token=").append(it) }
        }

        db = YdbDialectProvider.connect(url = jdbcUrl)

        if (tables.isNotEmpty()) {
            ydbTransaction(db) {
                runCatching { SchemaUtils.drop(*tables.toTypedArray()) }
                SchemaUtils.create(*tables.toTypedArray())
            }
        }
    }

    @AfterEach
    fun teardown() {
        if (!::db.isInitialized) return

        if (tables.isNotEmpty()) {
            runCatching {
                ydbTransaction(db) {
                    SchemaUtils.drop(*tables.toTypedArray())
                }
            }
        }

        runCatching { TransactionManager.closeAndUnregister(db) }
    }

    protected fun tx(block: JdbcTransaction.() -> Unit) = ydbTransaction(db, statement = block)

    companion object {
        @JvmField
        @RegisterExtension
        val ydb: YdbHelperExtension = YdbHelperExtension()
    }
}
