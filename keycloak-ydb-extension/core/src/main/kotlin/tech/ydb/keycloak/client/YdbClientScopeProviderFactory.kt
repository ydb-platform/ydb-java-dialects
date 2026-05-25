package tech.ydb.keycloak.client

import org.keycloak.connections.jpa.JpaConnectionProvider
import org.keycloak.models.ClientScopeProvider
import org.keycloak.models.KeycloakSession
import org.keycloak.models.jpa.JpaClientScopeProviderFactory
import tech.ydb.keycloak.config.ProviderConfig.PROVIDER_ID
import tech.ydb.keycloak.config.ProviderConfig.PROVIDER_PRIORITY
import tech.ydb.keycloak.realm.YdbRealmProvider

class YdbClientScopeProviderFactory : JpaClientScopeProviderFactory() {
  override fun create(session: KeycloakSession): ClientScopeProvider {
    val em = session.getProvider(JpaConnectionProvider::class.java).entityManager
    return YdbRealmProvider(session, em, null, null)
  }

  override fun order(): Int = PROVIDER_PRIORITY

  override fun getId(): String = PROVIDER_ID
}