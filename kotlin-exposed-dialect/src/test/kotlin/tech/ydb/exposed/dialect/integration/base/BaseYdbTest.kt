package tech.ydb.exposed.dialect.integration.base

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import tech.ydb.exposed.dialect.YDB_DRIVER_CLASS
import tech.ydb.exposed.dialect.registerYdbDialect
import tech.ydb.exposed.dialect.ydbTransaction
import tech.ydb.test.junit5.YdbHelperExtension
import java.sql.Connection

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

    protected open val enableSignedDatetimes: Boolean = false

    /** Appended to the JDBC URL after `disablePrepareDataQuery=true` (e.g. `&forceSignedDatetimes=true`). */
    protected open val jdbcUrlSuffix: String = ""

    @BeforeEach
    fun setupDatabase() {
        registerYdbDialect(enableSignedDatetimes)

        val jdbcUrl = buildString {
            append("jdbc:ydb:")
            append(if (ydb.useTls()) "grpcs://" else "grpc://")
            append(ydb.endpoint())
            append(ydb.database())
            append("?disablePrepareDataQuery=true")
            append(jdbcUrlSuffix)
            ydb.authToken()?.let { append("&token=").append(it) }
        }

        db = Database.connect(
            url = jdbcUrl,
            driver = YDB_DRIVER_CLASS,
            databaseConfig = DatabaseConfig {
                defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                useNestedTransactions = false
            }
        )

        if (tables.isNotEmpty()) {
            transaction(db) {
                runCatching { SchemaUtils.drop(*tables.toTypedArray()) }
                SchemaUtils.create(*tables.toTypedArray())
            }
        }
    }

    protected fun tx(block: JdbcTransaction.() -> Unit) = ydbTransaction(db, statement = block)

    companion object {
        @JvmField
        @RegisterExtension
        val ydb: YdbHelperExtension = YdbHelperExtension()
    }
}
