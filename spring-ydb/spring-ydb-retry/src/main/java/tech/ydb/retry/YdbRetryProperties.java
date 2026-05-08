package tech.ydb.retry;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ydb.transaction.retry")
public class YdbRetryProperties {

    private boolean enabled = YdbRetryPolicyConfig.DEFAULT_ENABLED;
    private int maxRetries = YdbRetryPolicyConfig.DEFAULT_MAX_RETRIES;
    private int slowBackoffBaseMs = YdbRetryPolicyConfig.DEFAULT_SLOW_BACKOFF_BASE_MS;
    private int fastBackoffBaseMs = YdbRetryPolicyConfig.DEFAULT_FAST_BACKOFF_BASE_MS;
    private int slowCapBackoffMs = YdbRetryPolicyConfig.DEFAULT_SLOW_CAP_BACKOFF_MS;
    private int fastCapBackoffMs = YdbRetryPolicyConfig.DEFAULT_FAST_CAP_BACKOFF_MS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getSlowBackoffBaseMs() {
        return slowBackoffBaseMs;
    }

    public void setSlowBackoffBaseMs(int slowBackoffBaseMs) {
        this.slowBackoffBaseMs = slowBackoffBaseMs;
    }

    public int getFastBackoffBaseMs() {
        return fastBackoffBaseMs;
    }

    public void setFastBackoffBaseMs(int fastBackoffBaseMs) {
        this.fastBackoffBaseMs = fastBackoffBaseMs;
    }

    public int getSlowCapBackoffMs() {
        return slowCapBackoffMs;
    }

    public void setSlowCapBackoffMs(int slowCapBackoffMs) {
        this.slowCapBackoffMs = slowCapBackoffMs;
    }

    public int getFastCapBackoffMs() {
        return fastCapBackoffMs;
    }

    public void setFastCapBackoffMs(int fastCapBackoffMs) {
        this.fastCapBackoffMs = fastCapBackoffMs;
    }

    public YdbRetryPolicyConfig toConfig() {
        return new YdbRetryPolicyConfig(
                enabled,
                maxRetries,
                slowBackoffBaseMs,
                fastBackoffBaseMs,
                slowCapBackoffMs,
                fastCapBackoffMs);
    }
}
