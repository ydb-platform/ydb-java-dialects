package tech.ydb.exposed.dialect.spring.boot.autoconfigure

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.spring.boot.autoconfigure.ExposedAutoConfiguration
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import tech.ydb.exposed.dialect.YdbDialect
import tech.ydb.exposed.dialect.registerYdbDialect
import tech.ydb.exposed.dialect.ydbDatabaseConfig
import javax.sql.DataSource

/**
 * YDB-specific layer on top of Exposed's Spring Boot starter.
 *
 * Exposed already knows how to wire its Spring transaction manager; this configuration only adds
 * the YDB dialect registration, YDB defaults, and a retry-aware transaction entrypoint.
 */
@AutoConfiguration(before = [DataSourceTransactionManagerAutoConfiguration::class])
@ConditionalOnClass(Database::class, YdbDialect::class, ExposedAutoConfiguration::class)
@Conditional(OnYdbJdbcUrlCondition::class)
@EnableConfigurationProperties(YdbExposedProperties::class)
class YdbExposedAutoConfiguration(
    applicationContext: ApplicationContext,
    private val properties: YdbExposedProperties
) : ExposedAutoConfiguration(applicationContext) {

    @Bean
    fun ydbDialectRegistration(): InitializingBean = InitializingBean {
        registerYdbDialect(properties.enableSignedDatetimes)
    }

    @Bean
    @ConditionalOnMissingBean(DatabaseConfig::class)
    override fun databaseConfig(): DatabaseConfig =
        ydbDatabaseConfig(enableSignedDatetimes = properties.enableSignedDatetimes)

    @Bean
    @ConditionalOnMissingBean(Database::class)
    fun database(dataSource: DataSource, databaseConfig: DatabaseConfig): Database {
        registerYdbDialect(properties.enableSignedDatetimes)
        return Database.connect(dataSource, databaseConfig = databaseConfig)
    }

    @Bean
    @ConditionalOnBean(Database::class)
    @ConditionalOnMissingBean
    fun ydbTransactionOperations(database: Database): YdbTransactionOperations =
        YdbTransactionOperations(database)
}
