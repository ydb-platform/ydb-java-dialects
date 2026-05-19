package tech.ydb.exposed.dialect.spring.boot.autoconfigure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.support.StaticApplicationContext
import tech.ydb.exposed.dialect.YdbDialect
import java.sql.Connection

class YdbExposedAutoConfigurationTest {

    @Test
    fun `creates database config aligned with YDB defaults`() {
        val autoConfiguration = YdbExposedAutoConfiguration(
            applicationContext = StaticApplicationContext(),
            properties = YdbExposedProperties()
        )

        val databaseConfig = autoConfiguration.databaseConfig()

        assertInstanceOf(YdbDialect::class.java, databaseConfig.explicitDialect)
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, databaseConfig.defaultIsolationLevel)
        assertFalse(databaseConfig.defaultReadOnly)
        assertFalse(databaseConfig.useNestedTransactions)
    }

    @Test
    fun `propagates signed datetime mode into explicit dialect`() {
        val autoConfiguration = YdbExposedAutoConfiguration(
            applicationContext = StaticApplicationContext(),
            properties = YdbExposedProperties(enableSignedDatetimes = true)
        )

        val databaseConfig = autoConfiguration.databaseConfig()
        val dialect = databaseConfig.explicitDialect as YdbDialect

        assertTrue(dialect.enableSignedDatetimes)
    }
}
