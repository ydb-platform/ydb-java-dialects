package tech.ydb.retry.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tech.ydb.test.junit5.YdbHelperExtension;

/**
 * Integration tests use a single YDB environment and a deterministic error channel state, so they
 * must be performed sequentially one after the other.
 */
@YdbIntegrationTest
@Execution(ExecutionMode.SAME_THREAD)
public abstract class YdbDockerTest {

    public static final String INTEGRATION_TEST_LOCK = "ydb-integration-tests";

    @RegisterExtension
    static final YdbHelperExtension ydb = new YdbHelperExtension();

    @BeforeAll
    static void resetErrorChannel() {
        DeterministicErrorChannel.configure();
    }

    @DynamicPropertySource
    static void propertySource(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () ->
                        "jdbc:ydb:"
                                + (ydb.useTls() ? "grpcs://" : "grpc://")
                                + ydb.endpoint()
                                + ydb.database()
                                + "?channelInitializer=tech.ydb.retry.integration.DeterministicErrorChannel&"
                                + (ydb.authToken() != null ? "token=" + ydb.authToken() : ""));
    }
}
