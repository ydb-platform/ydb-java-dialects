package tech.ydb.keycloak.connection

import jakarta.persistence.EntityManager
import org.keycloak.provider.Provider

interface YdbConnectionProvider : Provider {
  val entityManager: EntityManager
}
