package tech.ydb.retry.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tech.ydb.test.junit5.YdbHelperExtension;

public abstract class YdbDockerTest {

    @RegisterExtension
    static final YdbHelperExtension ydb = new YdbHelperExtension();

    @BeforeAll
    static void resetErrorChannel() {
        DeterministicErrorChannel.configure();
    }

    @DynamicPropertySource
    static void propertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:ydb:" + (ydb.useTls() ? "grpcs://" : "grpc://") +
                        ydb.endpoint() + ydb.database()
                        + "?channelInitializer=tech.ydb.retry.integration.DeterministicErrorChannel&"
                        + (ydb.authToken() != null ? "token=" + ydb.authToken() : "")
        );
    }
}
