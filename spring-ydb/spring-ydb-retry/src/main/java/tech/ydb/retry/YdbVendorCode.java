package tech.ydb.retry;

/**
 * YDB status codes as JDBC {@link java.sql.SQLException#getErrorCode() vendor codes}.
 *
 * <p>Values match
 * <a href="https://github.com/ydb-platform/ydb-java-sdk/blob/master/core/src/main/java/tech/ydb/core/StatusCode.java">{@code tech.ydb.core.StatusCode}</a>
 * and
 * <a href="https://github.com/ydb-platform/ydb-java-sdk/blob/master/core/src/main/java/tech/ydb/core/Constants.java">{@code tech.ydb.core.Constants}</a>.
 * Kept as raw {@code int} constants so this module does not need {@code ydb-sdk-core} on the
 * runtime classpath.
 */
public final class YdbVendorCode {

    public static final int SERVER_STATUSES_FIRST = 400_000;
    public static final int TRANSPORT_STATUSES_FIRST = 401_000;
    public static final int INTERNAL_CLIENT_FIRST = 402_000;

    public static final int ABORTED = SERVER_STATUSES_FIRST + 40;
    public static final int UNAVAILABLE = SERVER_STATUSES_FIRST + 50;
    public static final int OVERLOADED = SERVER_STATUSES_FIRST + 60;
    public static final int TIMEOUT = SERVER_STATUSES_FIRST + 90;
    public static final int BAD_SESSION = SERVER_STATUSES_FIRST + 100;
    public static final int PRECONDITION_FAILED = SERVER_STATUSES_FIRST + 120;
    public static final int NOT_FOUND = SERVER_STATUSES_FIRST + 140;
    public static final int SESSION_EXPIRED = SERVER_STATUSES_FIRST + 150;
    public static final int UNDETERMINED = SERVER_STATUSES_FIRST + 170;
    public static final int SESSION_BUSY = SERVER_STATUSES_FIRST + 190;

    public static final int TRANSPORT_UNAVAILABLE = TRANSPORT_STATUSES_FIRST + 10;
    public static final int CLIENT_RESOURCE_EXHAUSTED = TRANSPORT_STATUSES_FIRST + 20;
    public static final int CLIENT_DEADLINE_EXCEEDED = TRANSPORT_STATUSES_FIRST + 30;
    public static final int CLIENT_INTERNAL_ERROR = TRANSPORT_STATUSES_FIRST + 50;
    public static final int CLIENT_CANCELLED = TRANSPORT_STATUSES_FIRST + 60;

    public static final int CLIENT_DISCOVERY_FAILED = INTERNAL_CLIENT_FIRST + 10;
    public static final int CLIENT_LIMITS_REACHED = INTERNAL_CLIENT_FIRST + 20;
    public static final int CLIENT_DEADLINE_EXPIRED = INTERNAL_CLIENT_FIRST + 30;
    public static final int CLIENT_GRPC_ERROR = INTERNAL_CLIENT_FIRST + 40;

    private YdbVendorCode() {
    }
}
