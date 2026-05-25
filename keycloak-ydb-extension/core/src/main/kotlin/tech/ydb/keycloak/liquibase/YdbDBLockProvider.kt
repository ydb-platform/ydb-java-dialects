package tech.ydb.keycloak.liquibase

import liquibase.Liquibase
import liquibase.exception.DatabaseException
import liquibase.exception.LiquibaseException
import org.jboss.logging.Logger
import org.keycloak.common.util.Retry
import org.keycloak.connections.jpa.JpaConnectionProvider
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProvider
import org.keycloak.connections.jpa.updater.liquibase.lock.LockRetryException
import org.keycloak.models.KeycloakSession
import org.keycloak.models.dblock.DBLockProvider
import org.keycloak.models.utils.KeycloakModelUtils
import tech.ydb.keycloak.connection.YdbConnectionProviderFactoryImpl
import java.sql.Connection
import java.sql.SQLException

class YdbDBLockProvider(
  private val factory: YdbDBLockProviderFactory,
  private val session: KeycloakSession
) : DBLockProvider {
  private companion object {
    private const val DEFAULT_MAX_ATTEMPTS = 10
    private const val INTERVAL_BASE_MILLIS = 10
  }

  private val logger: Logger = Logger.getLogger(YdbDBLockProvider::class.java)

  private var lockService: YdbLockService? = null
  private var dbConnection: Connection? = null
  private var initialized = false
  private var namespaceLocked: DBLockProvider.Namespace? = null

  private fun lazyInit() {
    if (!initialized) {
      val liquibaseProvider = session.getProvider(LiquibaseConnectionProvider::class.java)
      val jpaProviderFactory = session.keycloakSessionFactory
        .getProviderFactory(JpaConnectionProvider::class.java) as YdbConnectionProviderFactoryImpl

      this.dbConnection = jpaProviderFactory.connection
      val defaultSchema = jpaProviderFactory.schema

      try {
        val liquibase: Liquibase = liquibaseProvider.getLiquibase(dbConnection, defaultSchema)

        this.lockService = YdbLockService().apply {
          setChangeLogLockWaitTime(factory.lockWaitTimeoutMillis)
          setDatabase(liquibase.database)
        }
        initialized = true
      } catch (e: LiquibaseException) {
        safeRollbackConnection()
        safeCloseConnection()
        throw IllegalStateException(e)
      }
    }
  }

  // Assumed transaction was rolled-back and we want to start with new DB connection
  private fun restart() {
    safeCloseConnection()
    lazyInit()
  }

  override fun waitForLock(lock: DBLockProvider.Namespace) {
    KeycloakModelUtils.suspendJtaTransaction(session.keycloakSessionFactory) {
      lazyInit()
      if (checkNotNull(lockService).hasChangeLogLock()) {
        if (lock == this.namespaceLocked) {
          logger.warn("Locking namespace $lock which was already locked in this provider")
          return@suspendJtaTransaction
        } else {
          throw RuntimeException(String.format("Trying to get a lock when one was already taken by the provider"))
        }
      }

      logger.debug("Going to lock namespace=$lock")
      Retry.executeWithBackoff({
        checkNotNull(lockService).waitForLock(lock)
        namespaceLocked = lock
      }, { iteration: Int, e: Throwable? ->
        if (e is LockRetryException && iteration < (DEFAULT_MAX_ATTEMPTS - 1)) {
          // Indicates we should try to acquire lock again in different transaction
          safeRollbackConnection()
          restart()
        } else {
          safeRollbackConnection()
          safeCloseConnection()
        }
      }, DEFAULT_MAX_ATTEMPTS, INTERVAL_BASE_MILLIS)
    }
  }

  override fun releaseLock() {
    KeycloakModelUtils.suspendJtaTransaction(session.keycloakSessionFactory) {
      lazyInit()
      logger.debug("Going to release database lock namespace=$namespaceLocked")
      val (lockId, service) = checkLockBeforeRelease() ?: return@suspendJtaTransaction
      try {
        Retry.executeWithBackoff({ iteration: Int ->
          logger.debug("Release lock attempt ${iteration + 1}")
          service.tryReleaseLock(lockId)
        }, { iteration: Int, _: Throwable? ->
          if (iteration < DEFAULT_MAX_ATTEMPTS - 1) {
            safeRollbackConnection()
          } else {
            safeRollbackConnection()
            safeCloseConnection()
          }
        }, DEFAULT_MAX_ATTEMPTS, INTERVAL_BASE_MILLIS)
      } catch (e: RuntimeException) {
        logger.error("Failed to release lock after $DEFAULT_MAX_ATTEMPTS attempts", e)
      } finally {
        namespaceLocked = null
        service.cleanupLockState()
        service.reset()
      }
    }
  }

  override fun destroyLockInfo() {
    KeycloakModelUtils.suspendJtaTransaction(session.keycloakSessionFactory) {
      lazyInit()
      try {
        checkNotNull(lockService).destroy()
        checkNotNull(dbConnection).commit()
        logger.debug("Destroyed lock table")
      } catch (_: DatabaseException) {
        logger.error("Failed to destroy lock table")
        safeRollbackConnection()
      } catch (_: SQLException) {
        logger.error("Failed to destroy lock table")
        safeRollbackConnection()
      }
    }
  }

  override fun getCurrentLock(): DBLockProvider.Namespace? = namespaceLocked

  override fun supportsForcedUnlock(): Boolean = false

  private fun safeRollbackConnection() {
    try {
      dbConnection?.rollback()
    } catch (se: SQLException) {
      logger.warn("Failed to rollback connection after error", se)
    }
  }

  private fun safeCloseConnection() {
    if (dbConnection != null) {
      try {
        dbConnection?.close()
        dbConnection = null
        lockService = null
        initialized = false
      } catch (e: SQLException) {
        logger.warn("Failed to close connection", e)
      }
    }
  }

  override fun close() {
    KeycloakModelUtils.suspendJtaTransaction(session.keycloakSessionFactory) {
      safeCloseConnection()
    }
  }

  private fun checkLockBeforeRelease(): Pair<Int, YdbLockService>? {
    val lockId = namespaceLocked?.id
    if (lockId == null) {
      logger.debug("releaseLock called but no lock was held by this provider")
      return null
    }
    val service = lockService
    if (service == null) {
      logger.error("releaseLock called but lockService is null, namespace=$namespaceLocked")
      namespaceLocked = null
      return null
    }
    if (!service.hasChangeLogLock()) {
      logger.error("releaseLock called but lockService has no lock, namespace=$namespaceLocked")
      namespaceLocked = null
      return null
    }
    return Pair(lockId, service)
  }
}
