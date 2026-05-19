package tech.ydb.exposed.dialect.spring.boot.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("spring.exposed.ydb")
data class YdbExposedProperties(
    /**
     * Enables signed temporal mode in both the Exposed dialect and the JDBC URL.
     */
    var enableSignedDatetimes: Boolean = false
)
