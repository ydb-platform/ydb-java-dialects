package tech.ydb.keycloak.proxy.utils

import io.ktor.http.HttpStatusCode

object YdbRetryableBody {
  private const val ERROR_CODE = "ydb_retryable"

  fun isRetryable503(status: HttpStatusCode, body: ByteArray): Boolean =
    status == HttpStatusCode.ServiceUnavailable && body.decodeToString().contains(ERROR_CODE)
}
