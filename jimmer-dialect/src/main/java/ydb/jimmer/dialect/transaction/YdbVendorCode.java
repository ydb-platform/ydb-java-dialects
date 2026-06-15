package ydb.jimmer.dialect.transaction;

import java.util.Set;

public final class YdbVendorCode {
    private YdbVendorCode() {}

    public static final int SERVER_STATUSES_FIRST = 400_000;
    public static final int TRANSPORT_STATUSES_FIRST = 401_000;

    public static final int ABORTED = SERVER_STATUSES_FIRST + 40;
    public static final int UNAVAILABLE = SERVER_STATUSES_FIRST + 50;
    public static final int OVERLOADED = SERVER_STATUSES_FIRST + 60;
    public static final int BAD_SESSION = SERVER_STATUSES_FIRST + 100;
    public static final int SESSION_BUSY = SERVER_STATUSES_FIRST + 190;

    public static final int CLIENT_RESOURCE_EXHAUSTED = TRANSPORT_STATUSES_FIRST + 20;

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
}
