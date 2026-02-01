package tech.ydb.keycloak.migration

import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.LiquibaseException
import liquibase.resource.ClassLoaderResourceAccessor
import org.jboss.logging.Logger
import java.sql.SQLException
import javax.sql.DataSource

object YdbMigrationManager {
  private const val CHANGELOG_FILE: String = "ydb/db.changelog-master.xml"

  private val logger = Logger.getLogger(YdbMigrationManager::class.java)

  fun migrate(dataSource: DataSource) {
    logger.info("Starting YDB migrations using Liquibase...")

    try {
      dataSource.connection.use { connection ->
        Liquibase(CHANGELOG_FILE, ClassLoaderResourceAccessor(), JdbcConnection(connection)).use { liquibase ->
          liquibase.update()
          logger.info("YDB migrations completed successfully")
        }
      }
    } catch (e: LiquibaseException) {
      logger.error("Failed to execute YDB migrations", e)

      throw SQLException("Failed to execute YDB migrations", e)
    }
  }
}
