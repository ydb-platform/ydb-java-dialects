package tech.ydb.keycloak.client

import org.keycloak.Config
import org.keycloak.authorization.fgap.AdminPermissionsSchema
import org.keycloak.common.Profile
import org.keycloak.connections.jpa.JpaConnectionProvider
import org.keycloak.models.ClientProvider
import org.keycloak.models.KeycloakSession
import org.keycloak.models.KeycloakSessionFactory
import org.keycloak.models.RealmModel.RealmAttributeUpdateEvent
import org.keycloak.models.jpa.JpaClientProviderFactory
import org.keycloak.models.jpa.entities.RealmAttributes
import org.keycloak.protocol.saml.SamlConfigAttributes
import org.keycloak.provider.ProviderEvent
import tech.ydb.keycloak.config.ProviderConfig.PROVIDER_ID
import tech.ydb.keycloak.config.ProviderConfig.PROVIDER_PRIORITY
import tech.ydb.keycloak.realm.YdbRealmProvider

class YdbClientProviderFactory : JpaClientProviderFactory() {

  private lateinit var clientSearchableAttributes: Set<String>

  override fun init(config: Config.Scope) {
    var searchableAttrsArr = config.getArray("searchableAttributes")?.toList()
    if (searchableAttrsArr == null) {
      val s = System.getProperty("keycloak.client.searchableAttributes")
      searchableAttrsArr = s?.split("\\s*,\\s*".toRegex())
    }
    val s = HashSet(REQUIRED_SEARCHABLE_ATTRIBUTES)
    if (searchableAttrsArr != null) {
      s.addAll(searchableAttrsArr)
    }
    clientSearchableAttributes = s.toSet()
  }

  override fun postInit(factory: KeycloakSessionFactory) {
    if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2)) {
      factory.register { event: ProviderEvent? ->
        if (event is RealmAttributeUpdateEvent) {
          if (event.attributeName == RealmAttributes.ADMIN_PERMISSIONS_ENABLED && event.attributeValue.toBoolean()) {
            val keycloakSession = event.keycloakSession
            val realm = event.realm
            AdminPermissionsSchema.SCHEMA.init(keycloakSession, realm)
          }
        }
      }
    }
  }

  override fun create(session: KeycloakSession): ClientProvider {
    val em = session.getProvider(JpaConnectionProvider::class.java).entityManager
    return YdbRealmProvider(session, em, clientSearchableAttributes, null)
  }

  override fun close() {
    // no operations
  }

  override fun order(): Int = PROVIDER_PRIORITY

  override fun getId(): String = PROVIDER_ID

  private companion object{
    private val REQUIRED_SEARCHABLE_ATTRIBUTES = listOf(
      "saml_idp_initiated_sso_url_name",
      SamlConfigAttributes.SAML_ARTIFACT_BINDING_IDENTIFIER,
      "jwt.credential.issuer",
      "jwt.credential.sub"
    )
  }
}
