package tech.ydb.keycloak.error

import jakarta.persistence.PersistenceException
import jakarta.ws.rs.core.Response
import org.hibernate.exception.GenericJDBCException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.keycloak.models.KeycloakSession
import org.mockito.kotlin.mock
import tech.ydb.keycloak.testsupport.YdbRetryableExceptionUtil
import tech.ydb.keycloak.utils.YdbRetryableResponses

class YdbRetryableExceptionMappersTest {

  @Test
  fun genericJdbcExceptionMapperReturns503ForYdbRetryableCause() {
    val retryable = YdbRetryableExceptionUtil.ydbRetryableException()
    val mapper = YdbRetryableGenericJdbcExceptionMapper()
    injectSession(mapper, mock())

    val response = mapper.toResponse(GenericJDBCException("jdbc error", retryable))

    assertEquals(503, response.status)
    assertRetryableBody(response)
  }

  @Test
  fun persistenceExceptionMapperReturns503ForYdbRetryableCause() {
    val retryable = YdbRetryableExceptionUtil.ydbRetryableException()
    val mapper = YdbRetryablePersistenceExceptionMapper()
    injectSession(mapper, mock())

    val response = mapper.toResponse(PersistenceException(retryable))

    assertEquals(503, response.status)
    assertRetryableBody(response)
  }

  private fun assertRetryableBody(response: Response) {
    val body = response.entity as String
    assertTrue(body.contains(YdbRetryableResponses.ERROR_CODE))
    assertTrue(body.contains(YdbRetryableResponses.CONTENTION_DESCRIPTION))
  }

  private fun injectSession(mapper: Any, session: KeycloakSession) {
    val field = mapper.javaClass.getDeclaredField("session")
    field.isAccessible = true
    field.set(mapper, session)
  }
}
