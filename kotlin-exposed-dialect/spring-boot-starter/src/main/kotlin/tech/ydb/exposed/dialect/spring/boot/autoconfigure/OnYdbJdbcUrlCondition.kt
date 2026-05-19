package tech.ydb.exposed.dialect.spring.boot.autoconfigure

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata
import tech.ydb.exposed.dialect.YDB_JDBC_URL_PREFIX

internal class OnYdbJdbcUrlCondition : Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val url = context.environment.getProperty(SPRING_DATASOURCE_URL_PROPERTY)
            ?.trim()
            ?: return false

        return url.startsWith(YDB_JDBC_URL_PREFIX)
    }
}

internal const val SPRING_DATASOURCE_URL_PROPERTY = "spring.datasource.url"
internal const val SPRING_DATASOURCE_DRIVER_PROPERTY = "spring.datasource.driver-class-name"
