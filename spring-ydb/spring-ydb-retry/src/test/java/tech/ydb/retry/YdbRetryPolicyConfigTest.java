package tech.ydb.retry;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.ydb.retry.YdbRetryPolicyConfig.DEFAULT_FAST_BACKOFF_BASE_MS;
import static tech.ydb.retry.YdbRetryPolicyConfig.DEFAULT_FAST_CAP_BACKOFF_MS;
import static tech.ydb.retry.YdbRetryPolicyConfig.DEFAULT_MAX_RETRIES;
import static tech.ydb.retry.YdbRetryPolicyConfig.DEFAULT_SLOW_BACKOFF_BASE_MS;
import static tech.ydb.retry.YdbRetryPolicyConfig.DEFAULT_SLOW_CAP_BACKOFF_MS;

class YdbRetryPolicyConfigTest extends InterceptorTestSupport {

    @Test
    void defaultConstructorShouldSetDefaultValues() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig();

        assertEquals(DEFAULT_MAX_RETRIES, config.getMaxRetries());
        assertEquals(DEFAULT_SLOW_BACKOFF_BASE_MS, config.getSlowBackoffBaseMs());
        assertEquals(DEFAULT_FAST_BACKOFF_BASE_MS, config.getFastBackoffBaseMs());
        assertEquals(DEFAULT_SLOW_CAP_BACKOFF_MS, config.getSlowCapBackoffMs());
        assertEquals(DEFAULT_FAST_CAP_BACKOFF_MS, config.getFastCapBackoffMs());
    }

    @Test
    void customConstructorShouldSetValues() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 5, 100, 20, 2000, 300);

        assertEquals(5, config.getMaxRetries());
        assertEquals(100, config.getSlowBackoffBaseMs());
        assertEquals(20, config.getFastBackoffBaseMs());
        assertEquals(2000, config.getSlowCapBackoffMs());
        assertEquals(300, config.getFastCapBackoffMs());
    }

    @Test
    void shouldThrowWhenMaxRetriesIsZero() {
        assertThrows(
                IllegalArgumentException.class, () -> new YdbRetryPolicyConfig(true, 0, 0, 0, 0, 0));
    }

    @Test
    void shouldThrowWhenMaxRetriesIsNegative() {
        assertThrows(
                IllegalArgumentException.class, () -> new YdbRetryPolicyConfig(true, -1, 0, 0, 0, 0));
    }

    @Test
    void shouldThrowWhenSlowBackoffBaseIsNegative() {
        assertThrows(
                IllegalArgumentException.class, () -> new YdbRetryPolicyConfig(true, 1, -1, 0, 0, 0));
    }

    @Test
    void shouldThrowWhenFastBackoffBaseIsNegative() {
        assertThrows(
                IllegalArgumentException.class, () -> new YdbRetryPolicyConfig(true, 1, 0, -1, 0, 0));
    }

    @Test
    void shouldThrowWhenSlowCapIsNegative() {
        assertThrows(
                IllegalArgumentException.class, () -> new YdbRetryPolicyConfig(true, 1, 0, 0, -1, 0));
    }

    @Test
    void shouldThrowWhenFastCapIsNegative() {
        assertThrows(
                IllegalArgumentException.class, () -> new YdbRetryPolicyConfig(true, 1, 0, 0, 0, -1));
    }

    @Test
    void mergeWithNullShouldReturnSameInstance() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig();
        assertSame(config, config.merge(null));
    }

    @Test
    void mergeWithDefaultAnnotationShouldKeepConfigValues() throws NoSuchMethodException {
        YdbRetryPolicyConfig original = new YdbRetryPolicyConfig(true, 5, 100, 20, 2000, 300);

        Method method = YdbTransactionalTestService.class.getMethod("defaultRetry");
        YdbTransactional annotation =
                AnnotatedElementUtils.findMergedAnnotation(method, YdbTransactional.class);

        YdbRetryPolicyConfig merged = original.merge(annotation);

        assertEquals(5, merged.getMaxRetries());
        assertEquals(100, merged.getSlowBackoffBaseMs());
        assertEquals(20, merged.getFastBackoffBaseMs());
        assertEquals(2000, merged.getSlowCapBackoffMs());
        assertEquals(300, merged.getFastCapBackoffMs());
    }

    @Test
    void mergeWithCustomAnnotationShouldOverride() throws NoSuchMethodException {
        YdbRetryPolicyConfig original = new YdbRetryPolicyConfig(true, 5, 100, 20, 2000, 300);

        Method method = YdbTransactionalTestService.class.getMethod("ydbNewTransactionSettings");
        YdbTransactional annotation =
                AnnotatedElementUtils.findMergedAnnotation(method, YdbTransactional.class);

        YdbRetryPolicyConfig merged = original.merge(annotation);

        assertEquals(100, merged.getMaxRetries());
        assertEquals(200, merged.getSlowBackoffBaseMs());
        assertEquals(10, merged.getFastBackoffBaseMs());
        assertEquals(10000, merged.getSlowCapBackoffMs());
        assertEquals(12, merged.getFastCapBackoffMs());
    }

    @Test
    void mergeWithPartialOverrideShouldOnlyChangeSpecifiedValues() throws NoSuchMethodException {
        YdbRetryPolicyConfig original = new YdbRetryPolicyConfig(true, 5, 100, 20, 2000, 300);

        Method method = YdbTransactionalTestService.class.getMethod("ydbCustomRetry");
        YdbTransactional annotation =
                AnnotatedElementUtils.findMergedAnnotation(method, YdbTransactional.class);

        YdbRetryPolicyConfig merged = original.merge(annotation);

        // only maxRetries should change
        assertEquals(2, merged.getMaxRetries());
        assertEquals(100, merged.getSlowBackoffBaseMs());
        assertEquals(20, merged.getFastBackoffBaseMs());
        assertEquals(2000, merged.getSlowCapBackoffMs());
        assertEquals(300, merged.getFastCapBackoffMs());
    }

    @Test
    void shouldThrowWhenYdbTransactionalMaxRetriesIsNegative() throws NoSuchMethodException {
        YdbRetryPolicyConfig original = new YdbRetryPolicyConfig(true, 5, 100, 20, 2000, 300);

        Method method = YdbTransactionalTestService.class.getMethod("ydbNegativeMaxRetries");
        YdbTransactional annotation =
                AnnotatedElementUtils.findMergedAnnotation(method, YdbTransactional.class);

        assertThrows(IllegalArgumentException.class, () -> original.merge(annotation));
    }

    @Test
    void shouldRejectZeroMaxRetriesAtAnnotationMergeTime() throws NoSuchMethodException {
        YdbRetryPolicyConfig original = new YdbRetryPolicyConfig(true, 5, 100, 20, 2000, 300);

        Method method = YdbTransactionalTestService.class.getMethod("ydbZeroMaxRetries");
        YdbTransactional annotation =
                AnnotatedElementUtils.findMergedAnnotation(method, YdbTransactional.class);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> original.merge(annotation));

        assertEquals(
                "maxRetries must not be 0; use enabled = false to disable retry", exception.getMessage());
    }

    @Test
    void getJitterShouldReturnValueWithinRange() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig();
        long bound = 100;
        for (int i = 0; i < 50; i++) {
            long jitter = config.getJitter(bound);
            assertTrue(jitter >= 0 && jitter <= bound);
        }
    }

    @Test
    void powShouldBeComputedFromCapValues() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 5, 100, 20, 2000, 300);

        int expectedSlowPow = Integer.SIZE - Integer.numberOfLeadingZeros(2000);
        int expectedFastPow = Integer.SIZE - Integer.numberOfLeadingZeros(300);

        assertEquals(expectedSlowPow, config.getSlowPow());
        assertEquals(expectedFastPow, config.getFastPow());
    }

    @Test
    void powForSmallCapShouldBeOne() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 1, 0, 0, 1, 1);
        assertEquals(1, config.getSlowPow());
        assertEquals(1, config.getFastPow());
    }

    @Test
    void powForCapTwoShouldBeTwo() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 1, 0, 0, 2, 2);
        assertEquals(2, config.getSlowPow());
        assertEquals(2, config.getFastPow());
    }

    @Test
    void powForZeroCapShouldBeZero() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(true, 1, 0, 0, 0, 0);
        assertEquals(0, config.getSlowPow());
        assertEquals(0, config.getFastPow());
    }

    @Test
    void defaultConstructorShouldSetEnabledTrue() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig();
        assertTrue(config.isEnabled());
    }

    @Test
    void constructorShouldSetEnabledFalse() {
        YdbRetryPolicyConfig config = new YdbRetryPolicyConfig(false, 5, 100, 20, 2000, 300);
        assertFalse(config.isEnabled());
    }

    @Test
    void mergeShouldPreserveEnabledFromBaseConfig() throws NoSuchMethodException {
        YdbRetryPolicyConfig original = new YdbRetryPolicyConfig(false, 5, 100, 20, 2000, 300);

        Method method = YdbTransactionalTestService.class.getMethod("ydbCustomRetry");
        YdbTransactional annotation =
                AnnotatedElementUtils.findMergedAnnotation(method, YdbTransactional.class);

        YdbRetryPolicyConfig merged = original.merge(annotation);

        assertFalse(merged.isEnabled());
    }

    @Test
    void mergeShouldKeepEnabledTrueWhenConfigEnabled() throws NoSuchMethodException {
        YdbRetryPolicyConfig original = new YdbRetryPolicyConfig(true, 5, 100, 20, 2000, 300);

        Method method = YdbTransactionalTestService.class.getMethod("defaultRetry");
        YdbTransactional annotation =
                AnnotatedElementUtils.findMergedAnnotation(method, YdbTransactional.class);

        YdbRetryPolicyConfig merged = original.merge(annotation);

        assertTrue(merged.isEnabled());
    }

    @Test
    void mergeWithDefaultAnnotationShouldKeepEnabledFalseWhenConfigDisabled()
            throws NoSuchMethodException {
        YdbRetryPolicyConfig original = new YdbRetryPolicyConfig(false, 5, 100, 20, 2000, 300);

        Method method = YdbTransactionalTestService.class.getMethod("defaultRetry");
        YdbTransactional annotation =
                AnnotatedElementUtils.findMergedAnnotation(method, YdbTransactional.class);

        YdbRetryPolicyConfig merged = original.merge(annotation);

        assertFalse(merged.isEnabled());
    }

    @Test
    void mergeWithDisabledAnnotationShouldSetEnabledFalse() throws NoSuchMethodException {
        YdbRetryPolicyConfig original = new YdbRetryPolicyConfig(true, 5, 100, 20, 2000, 300);

        Method method = YdbTransactionalTestService.class.getMethod("ydbRetryDisabled");
        YdbTransactional annotation =
                AnnotatedElementUtils.findMergedAnnotation(method, YdbTransactional.class);

        YdbRetryPolicyConfig merged = original.merge(annotation);

        assertFalse(merged.isEnabled());
    }

    @Test
    void mergeWithEnabledAnnotationShouldNotOverrideDisabledGlobalConfig()
            throws NoSuchMethodException {
        YdbRetryPolicyConfig original = new YdbRetryPolicyConfig(false, 5, 100, 20, 2000, 300);

        Method method = YdbTransactionalTestService.class.getMethod("ydbRetryEnabled");
        YdbTransactional annotation =
                AnnotatedElementUtils.findMergedAnnotation(method, YdbTransactional.class);

        YdbRetryPolicyConfig merged = original.merge(annotation);

        assertFalse(merged.isEnabled());
    }
}
