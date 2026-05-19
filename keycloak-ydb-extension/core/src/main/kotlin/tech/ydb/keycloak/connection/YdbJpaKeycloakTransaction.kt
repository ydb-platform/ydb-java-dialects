package tech.ydb.keycloak.connection

import jakarta.persistence.EntityManager
import org.jboss.logging.Logger
import org.keycloak.connections.jpa.JpaKeycloakTransaction
import tech.ydb.keycloak.utils.YdbRetryableResponses
import tech.ydb.keycloak.utils.isYdbRetryable

class YdbJpaKeycloakTransaction(em: EntityManager) : JpaKeycloakTransaction(em) {

  override fun commit() {
    try {
      super.commit()
    } catch (e: Exception) {
      if (isYdbRetryable(e)) {
        LOG.warn("YDB retryable error during commit, returning 503")
        throw YdbRetryableResponses.toWebApplicationException(
          e,
          "YDB transaction aborted",
          YdbRetryableResponses.TRANSACTION_DESCRIPTION,
        )
      }
      throw e
    }
  }

  companion object {
    private val LOG: Logger = Logger.getLogger(YdbJpaKeycloakTransaction::class.java)
  }
}
