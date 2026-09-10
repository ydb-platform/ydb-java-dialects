package ydb.jimmer.dialect.transaction;

import java.sql.SQLException;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Decides whether a YDB error should be retried and how long to wait before the next attempt.
 * Logic is ported one-to-one from {@code spring-ydb-retry}'s {@code YdbRetryPolicy.kt}.
 *
 * <ul>
 *   <li>{@link YdbDelayCalculator#fullJitterMillis} &mdash; {@code Aborted},
 *       {@code Undetermined}</li>
 *   <li>{@link YdbDelayCalculator#equalJitterMillis} on the fast tier &mdash;
 *       {@code Unavailable}, transport errors, {@code CLIENT_GRPC_ERROR}</li>
 *   <li>{@link YdbDelayCalculator#equalJitterMillis} on the slow tier &mdash;
 *       {@code Overloaded}, {@code CLIENT_RESOURCE_EXHAUSTED}</li>
 *   <li>zero delay &mdash; {@code BadSession}, {@code SessionBusy}, {@code SessionExpired}</li>
 * </ul>
 *
 * <p>Not retried here (add handling if your workload needs it): {@code TIMEOUT},
 * {@code CLIENT_DEADLINE_EXPIRED}, {@code PRECONDITION_FAILED}, and other vendor codes.
 * Setting {@code idempotent = true} retries any code returned by this policy, not only
 * {@link #isTransientVendorCode}. {@code SESSION_EXPIRED} is retried with zero backoff only when
 * the call is marked idempotent and is not in the transient set.
 */
public final class YdbRetryPolicy {
    private YdbRetryPolicy() {}

    /**
     * These error codes are retried even when the call is not idempotent.
     */
    public static final Set<Integer> TRANSIENT_VENDOR_CODES = Set.of(
            YdbVendorCode.ABORTED,
            YdbVendorCode.UNAVAILABLE,
            YdbVendorCode.OVERLOADED,
            YdbVendorCode.BAD_SESSION,
            YdbVendorCode.SESSION_BUSY,
            YdbVendorCode.CLIENT_RESOURCE_EXHAUSTED
    );

    /**
     * Returns {@code true} if the vendor code is in the always-retried (transient) set.
     */
    public static boolean isTransientVendorCode(int vendorCode) {
        return TRANSIENT_VENDOR_CODES.contains(vendorCode);
    }

    /**
     * Walks the cause chain of {@code error} looking for a {@link SQLException} with a non-zero
     * vendor code. Mirrors kotlin-exposed's {@code extractVendorCode}.
     *
     * @return non-zero vendor code, or {@code 0} if no YDB-style code was found
     */
    public static int extractVendorCode(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                int vendorCode = sqlException.getErrorCode();
                if (vendorCode != 0) {
                    return vendorCode;
                }
            }
            current = current.getCause();
        }
        return 0;
    }

    public static OptionalLong getNextRetryDelayMs(int vendorCode, int attempt, RetryConfig config) {
        // {@code attempt} is the zero-based index of the attempt that has just failed, so the next
        // attempt is allowed only while we stay within the total {@code maxAttempts} budget
        // (which counts the initial execution).
        if (attempt + 1 >= config.maxAttempts()) {
            return OptionalLong.empty();
        }
        if (vendorCode == 0) {
            return OptionalLong.empty();
        }
        if (!config.idempotent() && !isTransientVendorCode(vendorCode)) {
            return OptionalLong.empty();
        }

        return switch (vendorCode) {
            case YdbVendorCode.BAD_SESSION, YdbVendorCode.SESSION_BUSY, YdbVendorCode.SESSION_EXPIRED ->
                    OptionalLong.of(0L);
            case YdbVendorCode.ABORTED, YdbVendorCode.UNDETERMINED ->
                    OptionalLong.of(YdbDelayCalculator.fullJitterMillis(
                            config.fastBackoffBaseMs(),
                            config.fastCapBackoffMs(),
                            config.fastCeiling(),
                            config.backoffMultiplier(),
                            attempt));
            case YdbVendorCode.UNAVAILABLE, YdbVendorCode.TRANSPORT_UNAVAILABLE, YdbVendorCode.CLIENT_GRPC_ERROR ->
                    OptionalLong.of(YdbDelayCalculator.equalJitterMillis(
                            config.fastBackoffBaseMs(),
                            config.fastCapBackoffMs(),
                            config.fastCeiling(),
                            config.backoffMultiplier(),
                            attempt));
            case YdbVendorCode.OVERLOADED, YdbVendorCode.CLIENT_RESOURCE_EXHAUSTED ->
                    OptionalLong.of(YdbDelayCalculator.equalJitterMillis(
                            config.slowBackoffBaseMs(),
                            config.slowCapBackoffMs(),
                            config.slowCeiling(),
                            config.backoffMultiplier(),
                            attempt));
            default -> OptionalLong.empty();
        };
    }
}
