package tech.ydb.lock.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tech.ydb.test.junit5.YdbHelperExtension;

/**
 * @author Kirill Kurdyukov
 */
@SpringBootTest(classes = TestApp.class)
public class YdbLockProviderTest {

    @RegisterExtension
    private static final YdbHelperExtension ydb = new YdbHelperExtension();

    @DynamicPropertySource
    private static void propertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", YdbLockProviderTest::jdbcUrl);
    }

    private static String jdbcUrl() {
        StringBuilder jdbc = new StringBuilder("jdbc:ydb:")
                .append(ydb.useTls() ? "grpcs://" : "grpc://")
                .append(ydb.endpoint())
                .append(ydb.database());

        if (ydb.authToken() != null) {
            jdbc.append("?").append("token=").append(ydb.authToken());
        }

        return jdbc.toString();
    }

    @Autowired
    private YdbLockProvider ydbLockProvider;

    @Test
    public void integrationTest() {

    }
}
