package tech.ydb.keycloak.realm

import org.jboss.logging.Logger
import org.keycloak.Config
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.models.RealmProviderFactory
import org.keycloak.provider.EnvironmentDependentProviderFactory
import tech.ydb.keycloak.config.ProviderPriority.PROVIDER_PRIORITY
import tech.ydb.keycloak.config.YdbProfile.IS_YDB_PROFILE_ENABLED
import tech.ydb.keycloak.connection.YdbConnectionProvider

class YdbRealmProviderFactory() : RealmProviderFactory<YdbRealmProvider>, EnvironmentDependentProviderFactory {

  private val logger = Logger.getLogger(YdbRealmProviderFactory::class.java)

  override fun create(session: KeycloakSession): YdbRealmProvider {
    val provider = session.getProvider(YdbConnectionProvider::class.java)?.let {
      YdbRealmProvider(session, it.entityManager)
    } ?: error("YdbConnectionProvider is not configured")

    logger.info("YdbRealmProvider successfully created")

    return provider
  }

  override fun init(scope: Config.Scope) {
    // no operations
  }

  override fun postInit(p0: KeycloakSessionFactory?) {
    // no operations
  }

  override fun close() {
    // no operations
  }

  override fun getId(): String = ID

  override fun isSupported(scope: Config.Scope): Boolean = IS_YDB_PROFILE_ENABLED

  override fun order(): Int = PROVIDER_PRIORITY + 1

  private companion object {
    private const val ID = "ydb-realm-provider-factory"
  }
}
