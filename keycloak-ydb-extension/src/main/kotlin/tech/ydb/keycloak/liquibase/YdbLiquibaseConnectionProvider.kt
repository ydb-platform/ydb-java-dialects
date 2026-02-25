package tech.ydb.keycloak.liquibase

import liquibase.database.AbstractJdbcDatabase
import liquibase.database.Database
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.resource.ResourceAccessor
import org.jboss.logging.Logger
import org.keycloak.Config
import org.keycloak.connections.jpa.updater.liquibase.conn.DefaultLiquibaseConnectionProvider
import org.keycloak.connections.jpa.updater.liquibase.conn.KeycloakLiquibase
import tech.ydb.keycloak.config.ProviderPriority.PROVIDER_PRIORITY
import tech.ydb.liquibase.database.YdbDatabase
import java.sql.Connection

class YdbLiquibaseConnectionProvider : DefaultLiquibaseConnectionProvider() {

  private var indexCreationThreshold: Long = DEFAULT_INDEX_CREATION_THRESHOLD

  override fun init(config: Config.Scope) {
    indexCreationThreshold = config.getLong("indexCreationThreshold", DEFAULT_INDEX_CREATION_THRESHOLD)
    logger.debugf("indexCreationThreshold is %d", indexCreationThreshold)
  }

  override fun getId(): String = PROVIDER_ID

  override fun order(): Int = PROVIDER_PRIORITY

  override fun getLiquibase(connection: Connection, defaultSchema: String?): KeycloakLiquibase {
    val database = newYdbDatabase(connection)
    if (!defaultSchema.isNullOrBlank()) {
      database.defaultSchemaName = defaultSchema
    }
    val resourceAccessor: ResourceAccessor = ClassLoaderResourceAccessor(javaClass.classLoader)
    logger.debugf(
      "Using YDB Liquibase changelog %s and changelogTableName %s",
      YDB_MASTER_CHANGELOG,
      database.databaseChangeLogTableName
    )
    (database as AbstractJdbcDatabase).set(
      INDEX_CREATION_THRESHOLD_PARAM,
      indexCreationThreshold
    )
    return KeycloakLiquibase(YDB_MASTER_CHANGELOG, resourceAccessor, database)
  }

  override fun getLiquibaseForCustomUpdate(
    connection: Connection,
    defaultSchema: String?,
    changelogLocation: String,
    classloader: ClassLoader,
    changelogTableName: String
  ): KeycloakLiquibase {
    val database = newYdbDatabase(connection)
    if (!defaultSchema.isNullOrBlank()) {
      database.defaultSchemaName = defaultSchema
    }
    val resourceAccessor = ClassLoaderResourceAccessor(classloader)
    database.databaseChangeLogTableName = changelogTableName

    logger.debugf("Using YDB Liquibase for custom update $changelogLocation and changelogTableName ${database.databaseChangeLogTableName}")

    return KeycloakLiquibase(changelogLocation, resourceAccessor, database)
  }

  private fun newYdbDatabase(connection: Connection): Database = YdbDatabase().also {
    it.connection = JdbcConnection(connection)
  }

  companion object {
    const val PROVIDER_ID: String = "ydb-liquibase"
    const val YDB_MASTER_CHANGELOG: String = "ydb/db.changelog-master.xml"

    private const val DEFAULT_INDEX_CREATION_THRESHOLD = 300000L
    private val logger = Logger.getLogger(YdbLiquibaseConnectionProvider::class.java)
  }
}