package tech.ydb.keycloak.error

import jakarta.persistence.PersistenceException
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.hibernate.exception.GenericJDBCException
import org.jboss.logging.Logger
import org.keycloak.models.KeycloakSession
import org.keycloak.services.error.KeycloakErrorHandler
import tech.ydb.keycloak.utils.YdbRetryableResponses
import tech.ydb.keycloak.utils.isYdbRetryable

@Provider
class YdbRetryableGenericJdbcExceptionMapper : ExceptionMapper<GenericJDBCException> {
  @Context
  private lateinit var session: KeycloakSession

  override fun toResponse(exception: GenericJDBCException): Response =
    mapOrDelegate(session, exception, "GenericJDBCException")
}

@Provider
class YdbRetryablePersistenceExceptionMapper : ExceptionMapper<PersistenceException> {
  @Context
  private lateinit var session: KeycloakSession

  override fun toResponse(exception: PersistenceException): Response =
    mapOrDelegate(session, exception, "PersistenceException")
}

private fun mapOrDelegate(session: KeycloakSession, exception: Throwable, label: String): Response {
  if (!isYdbRetryable(exception)) {
    return KeycloakErrorHandler.getResponse(session, exception)
  }
  LOG.warn("YDB retryable $label, returning 503")
  return YdbRetryableResponses.build503(exception, YdbRetryableResponses.CONTENTION_DESCRIPTION)
}

private val LOG: Logger = Logger.getLogger("tech.ydb.keycloak.error.YdbRetryableExceptionMappers")
