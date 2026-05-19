package tech.ydb.keycloak.connection

import jakarta.persistence.EntityManager
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import org.keycloak.connections.jpa.JpaKeycloakTransaction
import tech.ydb.keycloak.utils.isYdbRetryable

class YdbJpaKeycloakTransaction(em: EntityManager) : JpaKeycloakTransaction(em) {

  override fun commit() {
    try {
      super.commit()
    } catch (e: Exception) {
      if (isYdbRetryable(e)) {
        LOG.warn("YDB retryable error during commit, returning 503")
        throw WebApplicationException(
          "YDB transaction aborted due to contention",
          e,
          Response.status(Response.Status.SERVICE_UNAVAILABLE)
            .entity("""{"error":"ydb_retryable","error_description":"Transaction aborted, please retry"}""")
            .header("Retry-After", "1")
            .type(MediaType.APPLICATION_JSON_TYPE)
            .build()
        )
      }
      throw e
    }
  }

  companion object {
    private val LOG: Logger = Logger.getLogger(YdbJpaKeycloakTransaction::class.java)
  }
}
