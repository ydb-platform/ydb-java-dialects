package tech.ydb.keycloak.connection

import jakarta.persistence.EntityManager
import jakarta.ws.rs.WebApplicationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import tech.ydb.keycloak.testsupport.YdbRetryableExceptionUtil.ydbRetryableException

class YdbEntityManagerProxyTest {

  @Test
  fun retryableErrorFromDelegateIsMappedTo503() {
    val exception = ydbRetryableException("transaction conflict")
    val em = mock<EntityManager>()

    whenever(em.flush()).thenAnswer { throw exception }
    val proxy = YdbEntityManagerProxy.create(em)

    val ex = assertThrows(WebApplicationException::class.java) { proxy.flush() }

    assertEquals(503, ex.response.status)
    assertEquals("1", ex.response.getHeaderString("Retry-After"))
    assertEquals("application", ex.response.mediaType?.type)
    assertEquals("json", ex.response.mediaType?.subtype)
    assertSame(exception, ex.cause)
    val body = ex.response.entity as String
    assertTrue(body.contains("\"error\":\"ydb_retryable\""))
    assertTrue(body.contains("Transaction aborted due to contention, please retry"))
  }

  @Test
  fun retryableErrorInCauseChainIsMappedTo503() {
    val exception = ydbRetryableException()
    val em = mock<EntityManager>()

    whenever(em.flush()).thenThrow(RuntimeException("wrapper", exception))

    val proxy = YdbEntityManagerProxy.create(em)

    val ex = assertThrows(WebApplicationException::class.java) { proxy.flush() }

    assertEquals(503, ex.response.status)
    assertSame(exception, ex.cause?.cause)
  }

  @Test
  fun nonRetryableErrorIsRethrown() {
    val exception = IllegalStateException("db error")
    val em = mock<EntityManager>()
    whenever(em.flush()).thenThrow(exception)

    val proxy = YdbEntityManagerProxy.create(em)

    val ex = assertThrows(IllegalStateException::class.java) { proxy.flush() }

    assertSame(exception, ex)
  }

  @Test
  fun successfulInvocationIsDelegated() {
    val em = mock<EntityManager>()
    whenever(em.isOpen).thenReturn(true)

    val proxy = YdbEntityManagerProxy.create(em)

    assertTrue(proxy.isOpen)
  }
}
