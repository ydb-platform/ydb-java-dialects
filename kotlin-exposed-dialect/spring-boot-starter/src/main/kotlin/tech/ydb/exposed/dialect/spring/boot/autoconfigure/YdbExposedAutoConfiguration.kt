package tech.ydb.exposed.dialect.spring.boot.autoconfigure

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.spring.boot.autoconfigure.ExposedAutoConfiguration
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Primary
import tech.ydb.exposed.dialect.YdbDialect
import tech.ydb.exposed.dialect.registerYdbDialect
import javax.sql.DataSource

/**
 * Spring Boot bridge for the YDB Exposed dialect.
 *
 * The official Exposed Spring Boot starter knows how to integrate Exposed with Spring
 * transactions, but it does not know anything about YDB dialect registration, YDB defaults,
 * or the JDBC flag required for signed temporal mode. This auto-configuration layers those
 * pieces on top as a separate optional artifact.
 */
@AutoConfiguration
@ConditionalOnClass(Database::class, YdbDialect::class, ExposedAutoConfiguration::class)
@Conditional(OnYdbJdbcUrlCondition::class)
@EnableConfigurationProperties(YdbExposedProperties::class)
class YdbExposedAutoConfiguration {

    @Bean
    fun ydbDialectRegistration(properties: YdbExposedProperties): InitializingBean = InitializingBean {
        registerYdbDialect(properties.enableSignedDatetimes)
    }

    @Bean
    @Primary
    fun ydbDatabaseConfig(properties: YdbExposedProperties): DatabaseConfig =
        ydbStarterDatabaseConfig(enableSignedDatetimes = properties.enableSignedDatetimes)

    @Bean
    @DependsOn("ydbDialectRegistration")
    @ConditionalOnMissingBean(Database::class)
    fun database(dataSource: DataSource, ydbDatabaseConfig: DatabaseConfig): Database =
        Database.connect(
            datasource = dataSource,
            databaseConfig = ydbDatabaseConfig
        )

    @Bean
    @ConditionalOnBean(Database::class)
    @ConditionalOnMissingBean
    fun ydbTransactionOperations(database: Database): YdbTransactionOperations =
        YdbTransactionOperations(database)
}
