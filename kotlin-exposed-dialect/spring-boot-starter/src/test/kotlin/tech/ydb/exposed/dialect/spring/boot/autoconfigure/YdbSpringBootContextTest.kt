package tech.ydb.exposed.dialect.spring.boot.autoconfigure

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import tech.ydb.exposed.dialect.YdbDialect
import tech.ydb.exposed.dialect.createYdbStatement
import tech.ydb.test.junit5.YdbHelperExtension
import java.sql.Connection

@SpringBootTest(
    classes = [YdbSpringBootContextTest.TestApplication::class],
    properties = [
        "spring.exposed.ydb.enable-signed-datetimes=true"
    ]
)
class YdbSpringBootContextTest {

    object SpringBootTable : Table("spring_boot_starter_table") {
        val id = integer("id")
        val name = varchar("name", 128)

        override val primaryKey = PrimaryKey(id)
        override fun createStatement(): List<String> = createYdbStatement()
    }

    @Autowired
    private lateinit var database: Database

    @Autowired
    private lateinit var databaseConfig: DatabaseConfig

    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var ydbTx: YdbTransactionOperations

    @AfterEach
    fun tearDown() {
        if (!::ydbTx.isInitialized) return

        runCatching {
            ydbTx.execute {
                SchemaUtils.drop(SpringBootTable)
            }
        }
    }

    @Test
    fun `spring boot context wires YDB database and config`() {
        val dialect = assertInstanceOf(YdbDialect::class.java, database.dialect)
        assertTrue(dialect.enableSignedDatetimes)
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, databaseConfig.defaultIsolationLevel)
        assertFalse(databaseConfig.useNestedTransactions)
        assertTrue(
            environment.getProperty("spring.datasource.url")
                ?.contains("forceSignedDatetimes=true") == true
        )
    }

    @Test
    fun `spring boot context can run CRUD through YdbTransactionOperations`() {
        ydbTx.execute {
            runCatching { SchemaUtils.drop(SpringBootTable) }
            SchemaUtils.create(SpringBootTable)

            SpringBootTable.insert {
                it[id] = 1
                it[name] = "spring-boot"
            }
        }

        val actual = ydbTx.execute(readOnly = true) {
            SpringBootTable.selectAll().single()[SpringBootTable.name]
        }

        assertEquals("spring-boot", actual)
    }

    @SpringBootApplication
    class TestApplication

    companion object {
        @JvmField
        @RegisterExtension
        val ydb: YdbHelperExtension = YdbHelperExtension()

        @JvmStatic
        @DynamicPropertySource
        fun springDatasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") {
                ydbStarterJdbcUrl(
                    url = buildJdbcUrl(),
                    enableSignedDatetimes = true
                )
            }
            registry.add("spring.datasource.driver-class-name") { "tech.ydb.jdbc.YdbDriver" }
        }

        private fun buildJdbcUrl(): String = buildString {
            append("jdbc:ydb:")
            append(if (ydb.useTls()) "grpcs://" else "grpc://")
            append(ydb.endpoint())
            append(ydb.database())

            val params = mutableListOf(
                "disablePrepareDataQuery=true",
                "disableAutoPreparedBatches=true"
            )

            ydb.authToken()?.let { params += "token=$it" }

            append("?")
            append(params.joinToString("&"))
        }
    }
}
