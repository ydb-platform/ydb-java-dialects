package tech.ydb.keycloak.utils

import tech.ydb.jdbc.exception.YdbRetryableException

fun isYdbRetryable(t: Throwable): Boolean {
  var cause: Throwable? = t
  while (cause != null) {
    if (cause is YdbRetryableException) return true
    cause = cause.cause
  }
  return false
}
