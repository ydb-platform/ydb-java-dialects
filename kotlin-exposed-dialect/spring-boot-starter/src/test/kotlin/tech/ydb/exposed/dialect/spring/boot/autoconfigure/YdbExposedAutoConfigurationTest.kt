package tech.ydb.exposed.dialect.spring.boot.autoconfigure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.ydb.exposed.dialect.YdbDialect
import java.sql.Connection

class YdbExposedAutoConfigurationTest {

    @Test
    fun `creates database config aligned with YDB defaults`() {
        val autoConfiguration = YdbExposedAutoConfiguration()
        val databaseConfig = autoConfiguration.ydbDatabaseConfig(YdbExposedProperties())

        assertInstanceOf(YdbDialect::class.java, databaseConfig.explicitDialect)
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, databaseConfig.defaultIsolationLevel)
        assertFalse(databaseConfig.defaultReadOnly)
        assertFalse(databaseConfig.useNestedTransactions)
    }

    @Test
    fun `propagates signed datetime mode into explicit dialect`() {
        val autoConfiguration = YdbExposedAutoConfiguration()
        val databaseConfig = autoConfiguration.ydbDatabaseConfig(
            YdbExposedProperties(enableSignedDatetimes = true)
        )
        val dialect = databaseConfig.explicitDialect as YdbDialect

        assertTrue(dialect.enableSignedDatetimes)
    }
}
