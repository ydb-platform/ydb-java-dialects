package tech.ydb.keycloak.connection

import jakarta.persistence.EntityManager
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import tech.ydb.keycloak.utils.isYdbRetryable
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class YdbEntityManagerProxy(private val em: EntityManager) {
  private fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
    try {
      return method.invoke(em, *(args ?: emptyArray()))
    } catch (e: InvocationTargetException) {
      val cause = e.cause ?: throw e
      if (isYdbRetryable(cause)) {
        LOG.warn("YDB retryable error during ${method.name}, returning 503")
        throw ydbRetryableResponse(cause)
      }
      throw cause
    } catch (e: Exception) {
      if (isYdbRetryable(e)) {
        LOG.warn("YDB retryable error during ${method.name}, returning 503")
        throw ydbRetryableResponse(e)
      }
      throw e
    }
  }

  private fun ydbRetryableResponse(cause: Throwable) = WebApplicationException(
    cause.message,
    cause,
    Response.status(Response.Status.SERVICE_UNAVAILABLE)
      .entity("""{"error":"ydb_retryable","error_description":"Transaction aborted due to contention, please retry"}""")
      .header("Retry-After", "1")
      .type(MediaType.APPLICATION_JSON_TYPE)
      .build()
  )

  companion object {
    private val LOG: Logger = Logger.getLogger(YdbEntityManagerProxy::class.java)

    fun create(em: EntityManager): EntityManager {
      val proxy = YdbEntityManagerProxy(em)
      return Proxy.newProxyInstance(
        EntityManager::class.java.classLoader,
        arrayOf(EntityManager::class.java),
        proxy::invoke
      ) as EntityManager
    }
  }
}
