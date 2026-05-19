package tech.ydb.exposed.dialect.integration.basic

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import tech.ydb.exposed.dialect.YDB_DRIVER_CLASS
import tech.ydb.exposed.dialect.YdbDialect
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.registerYdbDialect
import tech.ydb.exposed.dialect.ydbTransaction
import tech.ydb.test.junit5.YdbHelperExtension
import java.sql.Connection

/**
 * Verifies that after [registerYdbDialect] plain Exposed [Database.connect] works
 * with the same [DatabaseConfig] defaults as integration tests.
 */
class RegisterYdbDialectConnectIT {

    companion object {
        @JvmField
        @RegisterExtension
        val ydb: YdbHelperExtension = YdbHelperExtension()
    }

    object PlainConnectTable : YdbTable("register_ydb_dialect_plain_connect") {
        val id = integer("id")
        val label = varchar("label", 64)

        override val primaryKey = PrimaryKey(id)
    }

    private lateinit var jdbcUrl: String
    private lateinit var db: Database

    @BeforeEach
    fun setUp() {
        registerYdbDialect()

        jdbcUrl = buildString {
            append("jdbc:ydb:")
            append(if (ydb.useTls()) "grpcs://" else "grpc://")
            append(ydb.endpoint())
            append(ydb.database())
            append("?disablePrepareDataQuery=true")
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
    }

    @AfterEach
    fun tearDown() {
        if (!::db.isInitialized) return

        runCatching {
            ydbTransaction(db) {
                SchemaUtils.drop(PlainConnectTable)
            }
        }
        runCatching { TransactionManager.closeAndUnregister(db) }
    }

    @Test
    fun `Database connect resolves YdbDialect after registerYdbDialect`() {
        assertInstanceOf(YdbDialect::class.java, db.dialect)
    }

    @Test
    fun `Table ddl insert and select work with plain Database connect`() = ydbTransaction(db) {
        SchemaUtils.create(PlainConnectTable)

        PlainConnectTable.insert {
            it[id] = 1
            it[label] = "via-plain-connect"
        }

        assertEquals("via-plain-connect", PlainConnectTable.selectAll().single()[PlainConnectTable.label])

        val ddl = PlainConnectTable.ddl.joinToString(" ")
        assertTrue(ddl.contains("PRIMARY KEY (id)"))
        assertTrue(ddl.contains("label Text"))
    }

}
