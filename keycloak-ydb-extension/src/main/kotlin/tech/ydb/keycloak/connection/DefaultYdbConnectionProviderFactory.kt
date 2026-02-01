package tech.ydb.keycloak.connection

import tech.ydb.keycloak.migration.YdbMigrationManager.migrate
import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.jboss.logging.Logger
import org.keycloak.Config
import org.keycloak.connections.jpa.support.EntityManagerProxy
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.provider.EnvironmentDependentProviderFactory
import tech.ydb.keycloak.config.YdbProfile.IS_YDB_PROFILE_ENABLED
import tech.ydb.keycloak.transaction.YdbJpaTransaction
import tech.ydb.keycloak.util.EntityManagerUtils.createEntityManagerFactory
import tech.ydb.keycloak.util.hikariDataSource

class DefaultYdbConnectionProviderFactory : YdbConnectionProviderFactory<YdbConnectionProvider>,
  EnvironmentDependentProviderFactory {

  private val logger: Logger = Logger.getLogger(DefaultYdbConnectionProviderFactory::class.java)

  private lateinit var dataSource: HikariDataSource
  private lateinit var entityManagerFactory: EntityManagerFactory

  override fun create(session: KeycloakSession): YdbConnectionProvider =
    createYdbConnectionProvider(session)

  override fun init(scope: Config.Scope) {
    val jdbcUrl = scope["jdbcUrl"]
    val poolSize = scope.getInt("poolSize", 10)
    val connectionTimeout = scope.getLong("connectionTimeout", 5000L) // todo review
    val showSql = scope.getBoolean("showSql", false)
    val formatSql = scope.getBoolean("formatSql", true)

    dataSource = hikariDataSource(jdbcUrl, poolSize)

    entityManagerFactory = createEntityManagerFactory(dataSource, showSql, formatSql)

    logger.info("YDB connection pool, JOOQ DSLContext and EntityManager configured successfully")

    migrate(dataSource)
  }

  override fun postInit(factory: KeycloakSessionFactory) {
    // no operations
  }

  override fun close() {
    entityManagerFactory.close()
    dataSource.close()
  }

  override fun getId(): String = PROVIDER_ID

  override fun isSupported(scope: Config.Scope): Boolean = IS_YDB_PROFILE_ENABLED

  private fun createYdbConnectionProvider(session: KeycloakSession): YdbConnectionProvider {
    return object : YdbConnectionProvider {
      override val entityManager: EntityManager = createEntityManager(session)

      override fun close() {
        entityManager.close()
      }
    }
  }

  private fun createEntityManager(session: KeycloakSession): EntityManager {
    val em = entityManagerFactory.createEntityManager()

    val tx = YdbJpaTransaction(em)
    session.transactionManager.enlist(tx)

    return EntityManagerProxy.create(session, em, true)
  }

  private companion object {
    private const val PROVIDER_ID: String = "default"
  }
}