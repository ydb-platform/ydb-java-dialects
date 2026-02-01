package tech.ydb.keycloak.transaction

import jakarta.persistence.EntityManager
import org.keycloak.models.KeycloakTransaction

class YdbJpaTransaction(
  private val em: EntityManager
) : KeycloakTransaction {

  override fun begin() {
    em.transaction.begin()
  }

  override fun commit() {
    em.transaction.commit()
  }

  override fun rollback() {
    if (em.transaction.isActive) {
      em.transaction.rollback()
    }
  }

  override fun setRollbackOnly() {
    if (em.transaction.isActive) {
      em.transaction.setRollbackOnly()
    }
  }

  override fun getRollbackOnly(): Boolean = em.transaction.rollbackOnly

  override fun isActive(): Boolean = em.transaction.isActive
}
