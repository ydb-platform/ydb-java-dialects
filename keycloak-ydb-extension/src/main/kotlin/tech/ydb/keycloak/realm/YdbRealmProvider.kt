package tech.ydb.keycloak.realm

import jakarta.persistence.EntityManager
import org.keycloak.models.KeycloakSession
import org.keycloak.models.jpa.JpaRealmProvider

class YdbRealmProvider(
  session: KeycloakSession,
  entityManager: EntityManager,
) : JpaRealmProvider(session, entityManager, null, null)
