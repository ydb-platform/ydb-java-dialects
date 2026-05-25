package tech.ydb.keycloak.utils

import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

object YdbRetryableResponses {
  const val ERROR_CODE = "ydb_retryable"

  const val CONTENTION_DESCRIPTION = "Transaction aborted due to contention, please retry"
  const val TRANSACTION_DESCRIPTION = "Transaction aborted, please retry"

  fun build503(cause: Throwable, errorDescription: String): Response =
    Response.status(Response.Status.SERVICE_UNAVAILABLE)
      .entity("""{"error":"$ERROR_CODE","error_description":"$errorDescription"}""")
      .type(MediaType.APPLICATION_JSON_TYPE)
      .build()

  fun toWebApplicationException(
    cause: Throwable,
    message: String = cause.message ?: ERROR_CODE,
    errorDescription: String = CONTENTION_DESCRIPTION,
  ): WebApplicationException = WebApplicationException(message, cause, build503(cause, errorDescription))
}
