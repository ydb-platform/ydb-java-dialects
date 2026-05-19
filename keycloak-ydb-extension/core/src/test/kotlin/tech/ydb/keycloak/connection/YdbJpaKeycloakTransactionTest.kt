package tech.ydb.keycloak.connection

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityTransaction
import jakarta.persistence.PersistenceException
import jakarta.ws.rs.WebApplicationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.keycloak.models.ModelException
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.doReturn
import tech.ydb.keycloak.testsupport.YdbRetryableExceptionUtil

class YdbJpaKeycloakTransactionTest {

  @Test
  fun retryableCommitErrorIsMappedTo503() {
    val retryable = YdbRetryableExceptionUtil.ydbRetryableException("commit conflict")
    val tx = mock<EntityTransaction>()
    doAnswer { throw retryable }.whenever(tx).commit()
    val em = mock<EntityManager> {
      on { transaction } doReturn tx
    }
    val transaction = YdbJpaKeycloakTransaction(em)

    val ex = assertThrows(WebApplicationException::class.java) { transaction.commit() }

    assertEquals(503, ex.response.status)
    assertEquals("1", ex.response.getHeaderString("Retry-After"))
    assertEquals("application", ex.response.mediaType?.type)
    assertEquals("json", ex.response.mediaType?.subtype)
    assertSame(retryable, ex.cause)
    val body = ex.response.entity as String
    assertTrue(body.contains("\"error\":\"ydb_retryable\""))
    assertTrue(body.contains("Transaction aborted, please retry"))
  }

  @Test
  fun retryableErrorWrappedInPersistenceExceptionIsMappedTo503() {
    val retryable = YdbRetryableExceptionUtil.ydbRetryableException()
    val tx = mock<EntityTransaction> {
      on { commit() } doThrow PersistenceException(retryable)
    }
    val em = mock<EntityManager> {
      on { transaction } doReturn tx
    }
    val transaction = YdbJpaKeycloakTransaction(em)

    val ex = assertThrows(WebApplicationException::class.java) { transaction.commit() }

    assertEquals(503, ex.response.status)
    assertTrue(ex.cause is ModelException)
    assertSame(retryable, ex.cause?.cause)
  }

  @Test
  fun nonRetryableCommitErrorIsRethrown() {
    val failure = IllegalStateException("commit failed")
    val tx = mock<EntityTransaction> {
      on { commit() } doThrow failure
    }
    val em = mock<EntityManager> {
      on { transaction } doReturn tx
    }
    val transaction = YdbJpaKeycloakTransaction(em)

    val ex = assertThrows(IllegalStateException::class.java) { transaction.commit() }

    assertSame(failure, ex)
  }

  @Test
  fun successfulCommitDelegatesToEntityManager() {
    val tx = mock<EntityTransaction>()
    val em = mock<EntityManager> {
      on { transaction } doReturn tx
    }
    val transaction = YdbJpaKeycloakTransaction(em)

    transaction.commit()
  }
}
