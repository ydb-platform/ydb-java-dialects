package tech.ydb.exposed.dialect.spring.boot.autoconfigure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.mock.env.MockEnvironment

class YdbExposedEnvironmentPostProcessorTest {

    private val postProcessor = YdbExposedEnvironmentPostProcessor()

    @Test
    fun `normalizes ydb datasource url and default driver class`() {
        val environment = MockEnvironment()
            .withProperty(SPRING_DATASOURCE_URL_PROPERTY, "jdbc:ydb:grpc://localhost:2136/local")

        postProcessor.postProcessEnvironment(environment, SpringApplication())

        assertEquals(
            "jdbc:ydb:grpc://localhost:2136/local?forceSignedDatetimes=false",
            environment.getProperty(SPRING_DATASOURCE_URL_PROPERTY)
        )
        assertEquals(
            "tech.ydb.jdbc.YdbDriver",
            environment.getProperty(SPRING_DATASOURCE_DRIVER_PROPERTY)
        )
    }

    @Test
    fun `propagates signed temporal mode into datasource url`() {
        val environment = MockEnvironment()
            .withProperty(SPRING_DATASOURCE_URL_PROPERTY, "jdbc:ydb:grpc://localhost:2136/local?token=abc")
            .withProperty("spring.exposed.ydb.enable-signed-datetimes", "true")

        postProcessor.postProcessEnvironment(environment, SpringApplication())

        assertEquals(
            "jdbc:ydb:grpc://localhost:2136/local?token=abc&forceSignedDatetimes=true",
            environment.getProperty(SPRING_DATASOURCE_URL_PROPERTY)
        )
    }

    @Test
    fun `does not touch non ydb datasource`() {
        val environment = MockEnvironment()
            .withProperty(SPRING_DATASOURCE_URL_PROPERTY, "jdbc:h2:mem:testdb")

        postProcessor.postProcessEnvironment(environment, SpringApplication())

        assertEquals("jdbc:h2:mem:testdb", environment.getProperty(SPRING_DATASOURCE_URL_PROPERTY))
        assertNull(environment.getProperty(SPRING_DATASOURCE_DRIVER_PROPERTY))
    }
}
