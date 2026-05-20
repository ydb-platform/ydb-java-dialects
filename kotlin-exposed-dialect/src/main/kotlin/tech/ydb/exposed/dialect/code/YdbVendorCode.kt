package tech.ydb.exposed.dialect.code

/**
 * YDB status codes as JDBC [java.sql.SQLException.getErrorCode] vendor codes.
 *
 * Values match [tech.ydb.core.StatusCode](https://github.com/ydb-platform/ydb-java-sdk/blob/master/core/src/main/java/tech/ydb/core/StatusCode.java)
 * and [tech.ydb.core.Constants](https://github.com/ydb-platform/ydb-java-sdk/blob/master/core/src/main/java/tech/ydb/core/Constants.java).
 */
internal object YdbVendorCode {
    const val SERVER_STATUSES_FIRST: Int = 400_000
    const val TRANSPORT_STATUSES_FIRST: Int = 401_000
    const val INTERNAL_CLIENT_FIRST: Int = 402_000

    const val ABORTED: Int = SERVER_STATUSES_FIRST + 40
    const val UNAVAILABLE: Int = SERVER_STATUSES_FIRST + 50
    const val OVERLOADED: Int = SERVER_STATUSES_FIRST + 60
    const val TIMEOUT: Int = SERVER_STATUSES_FIRST + 90
    const val BAD_SESSION: Int = SERVER_STATUSES_FIRST + 100
    const val PRECONDITION_FAILED: Int = SERVER_STATUSES_FIRST + 120
    const val NOT_FOUND: Int = SERVER_STATUSES_FIRST + 140
    const val SESSION_EXPIRED: Int = SERVER_STATUSES_FIRST + 150
    const val UNDETERMINED: Int = SERVER_STATUSES_FIRST + 170
    const val SESSION_BUSY: Int = SERVER_STATUSES_FIRST + 190

    const val TRANSPORT_UNAVAILABLE: Int = TRANSPORT_STATUSES_FIRST + 10
    const val CLIENT_RESOURCE_EXHAUSTED: Int = TRANSPORT_STATUSES_FIRST + 20
    const val CLIENT_INTERNAL_ERROR: Int = TRANSPORT_STATUSES_FIRST + 50
    const val CLIENT_CANCELLED: Int = TRANSPORT_STATUSES_FIRST + 60

    const val CLIENT_DISCOVERY_FAILED: Int = INTERNAL_CLIENT_FIRST + 10
    const val CLIENT_LIMITS_REACHED: Int = INTERNAL_CLIENT_FIRST + 20
    const val CLIENT_DEADLINE_EXPIRED: Int = INTERNAL_CLIENT_FIRST + 30
    const val CLIENT_GRPC_ERROR: Int = INTERNAL_CLIENT_FIRST + 40
    const val CLIENT_DEADLINE_EXCEEDED: Int = TRANSPORT_STATUSES_FIRST + 30
}
