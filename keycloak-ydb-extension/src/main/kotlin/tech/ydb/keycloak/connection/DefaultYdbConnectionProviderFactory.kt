package tech.ydb.keycloak.connection

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.jboss.logging.Logger
import org.keycloak.Config
import org.keycloak.connections.jpa.DefaultJpaConnectionProvider
import org.keycloak.connections.jpa.JpaConnectionProvider
import org.keycloak.connections.jpa.JpaConnectionProviderFactory
import org.keycloak.connections.jpa.JpaKeycloakTransaction
import org.keycloak.connections.jpa.support.EntityManagerProxy
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.provider.EnvironmentDependentProviderFactory
import org.keycloak.provider.ServerInfoAwareProviderFactory
import tech.ydb.keycloak.config.YdbProfile.IS_YDB_PROFILE_ENABLED
import tech.ydb.keycloak.migration.YdbMigrationManager.migrate
import tech.ydb.keycloak.util.EntityManagerUtils.createEntityManagerFactory
import tech.ydb.keycloak.util.hikariDataSource
import java.sql.Connection

class DefaultYdbConnectionProviderFactory : JpaConnectionProviderFactory,
  ServerInfoAwareProviderFactory,
  EnvironmentDependentProviderFactory {

  private val logger: Logger = Logger.getLogger(DefaultYdbConnectionProviderFactory::class.java)

  private lateinit var dataSource: HikariDataSource
  private lateinit var entityManagerFactory: EntityManagerFactory

  override fun create(session: KeycloakSession): JpaConnectionProvider {
    val em = createEntityManager(session, entityManagerFactory)
    return DefaultJpaConnectionProvider(em)
  }

  override fun init(scope: Config.Scope) {
    if (!isSupported(scope)) {
      logger.debug("YDB JPA provider disabled (profile not enabled), skipping init")
      return
    }
    val jdbcUrl = resolveJdbcUrl(scope)
    if (jdbcUrl.isNullOrBlank()) {
      logger.warn("YDB JPA provider enabled but no JDBC URL configured. Set KC_SPI_CONNECTIONS_JPA_DEFAULT_JDBC_URL (or KC_DB_JDBC_URL) or legacy KC_SPI_YDB_CONNECTION_DEFAULT_JDBC_URL. Skipping init.")
      return
    }
    val poolSize = scope.getInt("poolSize", 10)
    val connectionTimeout = scope.getLong("connectionTimeout", 5000L) // todo review
    val showSql = scope.getBoolean("showSql", false)
    val formatSql = scope.getBoolean("formatSql", true)

    dataSource = hikariDataSource(jdbcUrl, poolSize)

    // TODO: maybe reuse JpaUtils.createEntityManagerFactory
    //  as it is inside DefaultJpaConnectionProviderFactory.lazyInit
    entityManagerFactory = createEntityManagerFactory(dataSource, showSql, formatSql)

    logger.info("YDB connection pool and EntityManagerFactory configured successfully")
    migrate(dataSource)
  }

  // TODO: simplify this
  private fun resolveJdbcUrl(scope: Config.Scope): String? =
    scope["jdbcUrl"]?.takeIf { it.isNotBlank() }
      ?: scope["url"]?.takeIf { it.isNotBlank() }
      ?: scope["jdbc-url"]?.takeIf { it.isNotBlank() }
      ?: System.getenv("KC_SPI_YDB_CONNECTION_DEFAULT_JDBC_URL")?.takeIf { it.isNotBlank() }
      ?: System.getenv("KC_DB_JDBC_URL")?.takeIf { it.isNotBlank() }
      ?: System.getenv("KC_DB_URL")?.takeIf { it.isNotBlank() }

  override fun postInit(factory: KeycloakSessionFactory) {
    // no operations
  }

  override fun close() {
    if (::entityManagerFactory.isInitialized) {
      entityManagerFactory.close()
    }
    if (::dataSource.isInitialized) {
      dataSource.close()
    }
  }

  override fun getId(): String = PROVIDER_ID

  override fun order(): Int = ORDER_YDB_FIRST

  override fun isSupported(scope: Config.Scope): Boolean = IS_YDB_PROFILE_ENABLED

  override fun getConnection(): Connection = dataSource.connection

  override fun getSchema(): String = schemaName

  override fun getOperationalInfo(): Map<String, String> = mapOf(
    "YDB" to "enabled",
    "Pool" to dataSource.hikariPoolMXBean.activeConnections.toString(),
  )

  private fun createEntityManager(session: KeycloakSession, emf: EntityManagerFactory): EntityManager {
    val em = emf.createEntityManager()

    val tx = JpaKeycloakTransaction(em)
    session.transactionManager.enlist(tx)

    return EntityManagerProxy.create(session, em, true)
  }

  private companion object {
    private const val PROVIDER_ID = "default"
    private const val ORDER_YDB_FIRST = 2
    private const val schemaName = "public"
  }
}
