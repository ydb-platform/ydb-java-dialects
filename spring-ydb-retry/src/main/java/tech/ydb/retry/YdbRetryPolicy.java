package tech.ydb.retry;

import java.sql.SQLException;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Decides whether a YDB error should be retried and how long to wait before the next attempt.
 * Logic is ported one-to-one from {@code kotlin-exposed-dialect}'s {@code YdbRetryPolicy.kt}.
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

    /**
     * Vendor codes retried even when the call is not marked idempotent. Matches
     * {@code TRANSIENT_VENDOR_CODES} in kotlin-exposed.
     */
    private static final Set<Integer> TRANSIENT_VENDOR_CODES = Set.of(
            YdbVendorCode.ABORTED,
            YdbVendorCode.UNAVAILABLE,
            YdbVendorCode.OVERLOADED,
            YdbVendorCode.CLIENT_RESOURCE_EXHAUSTED,
            YdbVendorCode.BAD_SESSION,
            YdbVendorCode.SESSION_BUSY);

    private YdbRetryPolicy() {
    }

    /** Returns {@code true} if the vendor code is in the always-retried (transient) set. */
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

    /**
     * Computes the backoff delay for the next retry attempt, or returns an empty optional when
     * the error must not be retried (either it is not a YDB error, the attempt budget is
     * exhausted, or the code is non-retryable for the configured idempotency).
     *
     * <p>Behaviour matches kotlin-exposed's {@code getNextRetryDelayMs} bit-for-bit.
     */
    public static OptionalLong getNextRetryDelayMs(
            int vendorCode, int attempt, YdbRetryPolicyConfig config, boolean idempotent) {
        // {@code attempt} is the zero-based index of the attempt that has just failed, so the next
        // attempt is allowed only while we stay within the total {@code maxAttempts} budget
        // (which counts the initial execution).
        if (attempt + 1 >= config.getMaxAttempts()) {
            return OptionalLong.empty();
        }
        if (vendorCode == 0) {
            return OptionalLong.empty();
        }
        if (!idempotent && !isTransientVendorCode(vendorCode)) {
            return OptionalLong.empty();
        }

        return switch (vendorCode) {
            case YdbVendorCode.BAD_SESSION, YdbVendorCode.SESSION_BUSY, YdbVendorCode.SESSION_EXPIRED ->
                    OptionalLong.of(0L);
            case YdbVendorCode.ABORTED, YdbVendorCode.UNDETERMINED ->
                    OptionalLong.of(YdbDelayCalculator.fullJitterMillis(
                            config.getFastBackoffBaseMs(),
                            config.getFastCapBackoffMs(),
                            config.getFastCeiling(),
                            attempt));
            case YdbVendorCode.UNAVAILABLE, YdbVendorCode.TRANSPORT_UNAVAILABLE, YdbVendorCode.CLIENT_GRPC_ERROR ->
                    OptionalLong.of(YdbDelayCalculator.equalJitterMillis(
                            config.getFastBackoffBaseMs(),
                            config.getFastCapBackoffMs(),
                            config.getFastCeiling(),
                            attempt));
            case YdbVendorCode.OVERLOADED, YdbVendorCode.CLIENT_RESOURCE_EXHAUSTED ->
                    OptionalLong.of(YdbDelayCalculator.equalJitterMillis(
                            config.getSlowBackoffBaseMs(),
                            config.getSlowCapBackoffMs(),
                            config.getSlowCeiling(),
                            attempt));
            default -> OptionalLong.empty();
        };
    }
}
