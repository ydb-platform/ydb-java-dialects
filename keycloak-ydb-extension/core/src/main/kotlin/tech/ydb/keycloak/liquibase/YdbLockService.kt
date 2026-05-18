package tech.ydb.keycloak.liquibase

import liquibase.Scope
import liquibase.exception.DatabaseException
import liquibase.executor.ExecutorService
import liquibase.lockservice.StandardLockService
import liquibase.statement.core.CreateDatabaseChangeLogLockTableStatement
import liquibase.statement.core.DeleteStatement
import liquibase.statement.core.LockDatabaseChangeLogStatement
import org.jboss.logging.Logger
import org.keycloak.common.util.Time
import org.keycloak.common.util.reflections.Reflections
import org.keycloak.connections.jpa.updater.liquibase.LiquibaseConstants
import org.keycloak.connections.jpa.updater.liquibase.lock.CustomLockDatabaseChangeLogStatement
import org.keycloak.connections.jpa.updater.liquibase.lock.LockRetryException
import org.keycloak.models.dblock.DBLockProvider
import tech.ydb.liquibase.lockservice.StandardLockServiceYdb

class YdbLockService : StandardLockServiceYdb() {
  private val log: Logger = Logger.getLogger(YdbLockService::class.java)

  override fun init() {
    val executor = Scope.getCurrentScope().getSingleton(ExecutorService::class.java)
      .getExecutor(LiquibaseConstants.JDBC_EXECUTOR, database)

    if (!isDatabaseChangeLogLockTableCreated) {
      try {
        log.trace("Create Database Lock Table")
        executor.execute(CreateDatabaseChangeLogLockTableStatement())
        database.commit()
      } catch (de: DatabaseException) {
        log.warn("Failed to create lock table. Maybe other transaction created in the meantime. Retrying...", de)
        log.trace(de.message, de)
        database.rollback()
        throw LockRetryException(de)
      }

      log.debug("Created database lock table")

      try {
        val field = Reflections.findDeclaredField(StandardLockService::class.java, "hasDatabaseChangeLogLockTable")
        Reflections.setAccessible(field)
        field.set(this@YdbLockService, true)
      } catch (iae: IllegalAccessException) {
        throw RuntimeException(iae)
      }
    }

    // Clean up any LOCKED=false rows left by manual intervention.
    // With INSERT/DELETE semantics, only LOCKED=true rows (actively held locks) should exist.
    try {
      executor.execute(
        DeleteStatement(database.liquibaseCatalogName, database.liquibaseSchemaName, database.databaseChangeLogLockTableName)
          .setWhere("LOCKED = false")
      )
      database.commit()
    } catch (de: DatabaseException) {
      database.rollback()
      throw LockRetryException(de)
    }
  }

  override fun waitForLock() {
    waitForLock(LockDatabaseChangeLogStatement())
  }

  fun waitForLock(lock: DBLockProvider.Namespace) {
    waitForLock(CustomLockDatabaseChangeLogStatement(lock.id))
  }

  private fun waitForLock(lockStmt: LockDatabaseChangeLogStatement) {
    var locked = false
    val startTime = Time.toMillis(Time.currentTime().toLong())
    val timeToGiveUp = startTime + (getChangeLogLockWaitTime())
    var nextAttempt = true

    while (nextAttempt) {
      locked = acquireLock(lockStmt)
      if (!locked) {
        val remainingTime = ((timeToGiveUp / 1000).toInt()) - Time.currentTime()
        if (remainingTime > 0) {
          log.debug("Will try to acquire log another time. Remaining time: $remainingTime seconds")
        } else {
          nextAttempt = false
        }
      } else {
        nextAttempt = false
      }
    }

    if (!locked) {
      val timeout = ((getChangeLogLockWaitTime() / 1000).toInt())
      throw IllegalStateException("Could not acquire change log lock within specified timeout $timeout seconds.  Currently locked by other transaction")
    }
  }

  override fun acquireLock(): Boolean {
    return acquireLock(LockDatabaseChangeLogStatement())
  }

  private fun acquireLock(lockStmt: LockDatabaseChangeLogStatement): Boolean {
    if (hasChangeLogLock) {
      return true
    }

    val executor = Scope.getCurrentScope().getSingleton(ExecutorService::class.java)
      .getExecutor(LiquibaseConstants.JDBC_EXECUTOR, database)

    try {
      database.rollback()
      this.init()
    } catch (de: DatabaseException) {
      throw IllegalStateException("Failed to retrieve lock", de)
    }

    val id = if (lockStmt is CustomLockDatabaseChangeLogStatement) lockStmt.id else DEFAULT_LOCK_ID

    return try {
      log.debug("Trying to acquire lock id=$id")
      executor.execute(YdbLockStatement(id))
      database.commit()

      hasChangeLogLock = true
      database.setCanCacheLiquibaseTableInfo(true)
      log.debug("Successfully acquired lock id=$id")
      true
    } catch (de: DatabaseException) {
      log.debug("Lock id=$id is held by another transaction, will retry. Details: ${de.message}")
      try {
        database.rollback()
      } catch (_: DatabaseException) {
        // no operations
      }
      false
    }
  }


  fun tryReleaseLock(lockId: Int) {
    log.debug("Going to release database lock id=$lockId")
    val executor = Scope.getCurrentScope().getSingleton(ExecutorService::class.java)
      .getExecutor(LiquibaseConstants.JDBC_EXECUTOR, database)

    database.rollback()
    try {
      executor.execute(YdbUnlockStatement(lockId))
      database.commit()
    } catch (e: Exception) {
      throw RuntimeException("Failed to release lock id=$lockId", e)
    }
  }

  fun cleanupLockState() {
    try {
      hasChangeLogLock = false
      database.setCanCacheLiquibaseTableInfo(false)
      database.rollback()
    } catch (_: DatabaseException) {
      // no operations
    }
  }

  override fun releaseLock() {
    releaseLock(DEFAULT_LOCK_ID)
  }

  fun releaseLock(lockId: Int) {
    try {
      if (hasChangeLogLock) {
        tryReleaseLock(lockId)
      } else {
        log.warn("Attempt to release lock, which is not owned by current transaction")
      }
    } catch (e: Exception) {
      log.error("Database error during release lock", e)
    } finally {
      cleanupLockState()
    }
  }

  companion object {
    private val DEFAULT_LOCK_ID = DBLockProvider.Namespace.DATABASE.id
  }
}
