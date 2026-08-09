package tech.ydb.trino;

/**
 * YDB vendor codes returned by the server.
 * These codes are returned as SQL error codes in SQLException.getErrorCode().
 */
public final class YdbVendorCode {
    // Server status codes
    public static final int ABORTED = 400040;
    public static final int UNAVAILABLE = 400050;
    public static final int OVERLOADED = 400060;
    public static final int TIMEOUT = 400090;
    public static final int BAD_SESSION = 400100;
    public static final int SESSION_EXPIRED = 400150;
    public static final int UNDETERMINED = 400170;
    public static final int SESSION_BUSY = 400190;
    public static final int TRANSPORT_UNAVAILABLE = 401050;
    public static final int CLIENT_GRPC_ERROR = 402010;
    public static final int CLIENT_RESOURCE_EXHAUSTED = 402020;

    private YdbVendorCode()
    {
    }
}