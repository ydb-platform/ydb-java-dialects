package tech.ydb.keycloak.realm

import org.jboss.logging.Logger
import org.keycloak.Config
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.models.RealmProviderFactory
import org.keycloak.connections.jpa.JpaConnectionProvider
import tech.ydb.keycloak.config.ProviderPriority.PROVIDER_PRIORITY

class YdbRealmProviderFactory() : RealmProviderFactory<YdbRealmProvider> {

  private val logger = Logger.getLogger(YdbRealmProviderFactory::class.java)

  override fun create(session: KeycloakSession): YdbRealmProvider {
    val provider = session.getProvider(JpaConnectionProvider::class.java)?.let {
      YdbRealmProvider(session, it.entityManager)
    } ?: error("JpaConnectionProvider is not configured in YDB")

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

  override fun order(): Int = PROVIDER_PRIORITY

  private companion object {
    private const val ID = "ydb-realm-provider-factory"
  }
}
