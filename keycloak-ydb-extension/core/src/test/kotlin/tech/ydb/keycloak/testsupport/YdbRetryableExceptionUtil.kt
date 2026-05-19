package tech.ydb.keycloak.testsupport

import tech.ydb.core.Status
import tech.ydb.core.StatusCode
import tech.ydb.core.UnexpectedResultException
import tech.ydb.jdbc.exception.ExceptionFactory
import tech.ydb.jdbc.exception.YdbRetryableException

object YdbRetryableExceptionUtil {
  fun ydbRetryableException(message: String = "contention"): YdbRetryableException {
    val unexpected = UnexpectedResultException(message, Status.of(StatusCode.ABORTED))
    return ExceptionFactory.createException(message, unexpected) as YdbRetryableException
  }
}
