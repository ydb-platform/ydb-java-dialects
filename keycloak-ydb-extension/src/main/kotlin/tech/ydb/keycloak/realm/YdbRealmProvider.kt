package tech.ydb.keycloak.realm

import jakarta.persistence.EntityManager
import org.keycloak.common.util.StackUtil
import org.keycloak.models.ClientModel
import org.keycloak.models.ClientModel.ClientRemovedEvent
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.models.jpa.JpaRealmProvider
import org.keycloak.models.jpa.RealmAdapter
import org.keycloak.models.jpa.entities.ClientEntity
import org.keycloak.models.jpa.entities.RealmEntity


/**
 * Realm provider for YDB. Overrides some functions to load the realm entity without
 * PESSIMISTIC_WRITE lock, because YDB does not support FOR UPDATE.
 */
class YdbRealmProvider(
  private val keycloakSession: KeycloakSession,
  entityManager: EntityManager,
  clientSearchableAttributes: Set<String>? = null,
  groupSearchableAttributes: Set<String>? = null,
) : JpaRealmProvider(
  keycloakSession,
  entityManager,
  clientSearchableAttributes,
  groupSearchableAttributes,
) {

  override fun removeRealm(id: String): Boolean {
    // YDB does not support FOR UPDATE (PESSIMISTIC_WRITE)
    val realm = em.find(RealmEntity::class.java, id) ?: return false
    val adapter = RealmAdapter(keycloakSession, em, realm)
    keycloakSession.users().preRemove(adapter)

    realm.defaultGroupIds.clear()
    em.flush()

    keycloakSession.clients().removeClients(adapter)
    em.createNamedQuery("deleteDefaultClientScopeRealmMappingByRealm")
      .setParameter("realm", realm).executeUpdate()

    keycloakSession.clientScopes().removeClientScopes(adapter)
    keycloakSession.roles().removeRoles(adapter)

    em.createNamedQuery("deleteOrganizationDomainsByRealm")
      .setParameter("realmId", realm.id).executeUpdate()
    em.createNamedQuery("deleteOrganizationsByRealm")
      .setParameter("realmId", realm.id).executeUpdate()
    keycloakSession.groups().preRemove(adapter)

    keycloakSession.identityProviders().removeAll()
    keycloakSession.identityProviders().removeAllMappers()

    em.createNamedQuery("removeClientInitialAccessByRealm")
      .setParameter("realm", realm).executeUpdate()

    em.remove(realm)
    em.flush()
    em.clear()

    val session = keycloakSession
    keycloakSession.keycloakSessionFactory.publish(object : RealmModel.RealmRemovedEvent {
      override fun getRealm(): RealmModel = adapter
      override fun getKeycloakSession(): KeycloakSession = session
    })
    return true
  }

  override fun removeClient(realm: RealmModel, id: String?): Boolean {
    logger.tracef("removeClient(%s, %s)%s", realm, id, StackUtil.getShortStackTrace())

    val client = getClientById(realm, id) ?: return false

    keycloakSession.users().preRemove(realm, client)
    keycloakSession.roles().removeRoles(client)

    // YDB does not support FOR UPDATE (PESSIMISTIC_WRITE)
    val clientEntity = em.find(ClientEntity::class.java, id)

    keycloakSession.keycloakSessionFactory.publish(object : ClientRemovedEvent {
      override fun getClient(): ClientModel {
        return client
      }

      override fun getKeycloakSession(): KeycloakSession {
        // without `this@YdbRealmProvider` here will be infinite recursion
        return this@YdbRealmProvider.keycloakSession
      }
    })

    val countRemoved = em.createNamedQuery("deleteClientScopeClientMappingByClient")
      .setParameter("clientId", clientEntity.id)
      .executeUpdate()

    // !!! comment from JpaRealmProvider)))
    // i have no idea why, but this needs to come before deleteScopeMapping
    em.remove(clientEntity)

    try {
      em.flush()
    } catch (e: RuntimeException) {
      logger.errorv("Unable to delete client entity: {0} from realm {1}", client.clientId, realm.name)
      throw e
    }

    return true
  }
}
