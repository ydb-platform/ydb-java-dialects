package tech.ydb.retry.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("integration")
@ResourceLock(YdbDockerTest.INTEGRATION_TEST_LOCK)
public @interface YdbIntegrationTest {
}
