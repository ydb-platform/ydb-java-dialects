package tech.ydb.exposed.dialect.integration.types

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import tech.ydb.exposed.dialect.YdbDialectProvider
import tech.ydb.exposed.dialect.YdbTable
import tech.ydb.exposed.dialect.ydbTransaction
import tech.ydb.test.junit5.YdbHelperExtension

/**
 * Verifies that opening the dialect with `forceLegacyDatetimes = true` emits
 * the legacy YDB temporal types (`Date`, `Datetime`, `Timestamp`).
 *
 * Doesn't extend [BaseYdbTest] because it needs to control the [YdbDialectProvider.connect]
 * call to flip the flag.
 */
class LegacyTemporalTypesIT {

    object LegacyTemporal : YdbTable("legacy_temporal_types") {
        val id = integer("id")
        val dateCol = date("date_col")
        val dateTimeCol = datetime("datetime_col")
        val timestampCol = timestamp("timestamp_col")

        override val primaryKey = PrimaryKey(id)
    }

    private lateinit var db: Database

    @BeforeEach
    fun setUp() {
        val jdbcUrl = buildString {
            append("jdbc:ydb:")
            append(if (ydb.useTls()) "grpcs://" else "grpc://")
            append(ydb.endpoint())
            append(ydb.database())
            ydb.authToken()?.let { append("?token=").append(it) }
        }
        db = YdbDialectProvider.connect(url = jdbcUrl, forceLegacyDatetimes = true)
    }

    @AfterEach
    fun tearDown() {
        if (!::db.isInitialized) return

        runCatching {
            ydbTransaction(db) {
                SchemaUtils.drop(LegacyTemporal)
            }
        }
        runCatching { TransactionManager.closeAndUnregister(db) }
    }

    @Test
    fun `forceLegacyDatetimes emits Date Datetime Timestamp`() = ydbTransaction(db) {
        val ddl = LegacyTemporal.ddl.joinToString(" ")
        assertTrue(ddl.contains("date_col Date") && !ddl.contains("Date32"), ddl)
        assertTrue(ddl.contains("datetime_col Datetime") && !ddl.contains("Datetime64"), ddl)
        assertTrue(ddl.contains("timestamp_col Timestamp") && !ddl.contains("Timestamp64"), ddl)
    }

    companion object {
        @JvmField
        @RegisterExtension
        val ydb: YdbHelperExtension = YdbHelperExtension()
    }
}
