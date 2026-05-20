package tech.ydb.keycloak.connection

import jakarta.persistence.EntityManager
import org.jboss.logging.Logger
import tech.ydb.keycloak.utils.YdbRetryableResponses
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
        throw YdbRetryableResponses.toWebApplicationException(cause)
      }
      throw cause
    } catch (e: Exception) {
      if (isYdbRetryable(e)) {
        LOG.warn("YDB retryable error during ${method.name}, returning 503")
        throw YdbRetryableResponses.toWebApplicationException(e)
      }
      throw e
    }
  }

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
