package tech.ydb.lock.provider;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Kirill Kurdyukov
 */
@ConfigurationProperties(prefix = "ydb.lock")
public class YdbLockProperties {

    public Duration acquireSemaphoreTimeout = Duration.ofSeconds(5);

    public void setAcquireSemaphoreTimeout(Duration acquireSemaphoreTimeout) {
        this.acquireSemaphoreTimeout = acquireSemaphoreTimeout;
    }

    public Duration getAcquireSemaphoreTimeout() {
        return acquireSemaphoreTimeout;
    }
}
