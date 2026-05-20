package tech.ydb.exposed.dialect.spring.boot.autoconfigure

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class YdbExposedEnvironmentPostProcessor : EnvironmentPostProcessor, Ordered {

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication
    ) {
        val rawUrl = environment.getProperty(SPRING_DATASOURCE_URL_PROPERTY)
            ?.trim()
            ?: return

        if (!rawUrl.startsWith(STARTER_YDB_JDBC_URL_PREFIX)) {
            return
        }

        val enableSignedDatetimes = environment.getProperty(
            "spring.exposed.ydb.enable-signed-datetimes",
            Boolean::class.java,
            false
        )

        val overrides = linkedMapOf<String, Any>(
            SPRING_DATASOURCE_URL_PROPERTY to ydbStarterJdbcUrl(
                url = rawUrl,
                enableSignedDatetimes = enableSignedDatetimes
            )
        )

        if (environment.getProperty(SPRING_DATASOURCE_DRIVER_PROPERTY).isNullOrBlank()) {
            overrides[SPRING_DATASOURCE_DRIVER_PROPERTY] = STARTER_YDB_DRIVER_CLASS
        }

        environment.propertySources.remove(PROPERTY_SOURCE_NAME)
        environment.propertySources.addFirst(MapPropertySource(PROPERTY_SOURCE_NAME, overrides))
    }

    private companion object {
        const val PROPERTY_SOURCE_NAME = "ydbExposedStarterOverrides"
    }
}
