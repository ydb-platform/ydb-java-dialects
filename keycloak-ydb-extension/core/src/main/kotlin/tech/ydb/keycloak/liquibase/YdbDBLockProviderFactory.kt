package tech.ydb.keycloak.liquibase

import org.jboss.logging.Logger
import org.keycloak.Config
import org.keycloak.common.util.Time
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.models.dblock.DBLockProviderFactory
import org.keycloak.provider.ProviderConfigProperty
import org.keycloak.provider.ProviderConfigurationBuilder
import tech.ydb.keycloak.config.ProviderConfig

class YdbDBLockProviderFactory : DBLockProviderFactory {
  private val logger: Logger = Logger.getLogger(YdbDBLockProviderFactory::class.java)

  var lockWaitTimeoutMillis: Long = 0
  private set

  override fun init(config: Config.Scope) {
    this.lockWaitTimeoutMillis = Time.toMillis(config.getLong("lockWaitTimeout", 900))
    logger.debug("Liquibase lock provider configured with lockWaitTime: $lockWaitTimeoutMillis seconds")
  }

  override fun create(session: KeycloakSession): YdbDBLockProvider {
    return YdbDBLockProvider(this, session)
  }

  override fun postInit(factory: KeycloakSessionFactory?) {
    // no operations
  }

  override fun setTimeouts(lockRecheckTimeMillis: Long, lockWaitTimeoutMillis: Long) {
    this.lockWaitTimeoutMillis = lockWaitTimeoutMillis
  }

  override fun close() {
    // no operations
  }

  override fun getConfigMetadata(): MutableList<ProviderConfigProperty?>? {
    return ProviderConfigurationBuilder.create()
      .property()
      .name("lockWaitTimeout")
      .type("int")
      .helpText("The maximum time to wait when waiting to release a database lock.")
      .add().build()
  }

  override fun order(): Int {
    return ProviderConfig.PROVIDER_PRIORITY
  }

  override fun getId(): String {
    return ProviderConfig.PROVIDER_ID
  }
}
