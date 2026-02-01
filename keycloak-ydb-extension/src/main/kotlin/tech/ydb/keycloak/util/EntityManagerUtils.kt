package tech.ydb.keycloak.util

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import org.hibernate.cfg.Configuration
import org.jboss.logging.Logger

object EntityManagerUtils {
  private val logger = Logger.getLogger(EntityManagerUtils::class.java)

  fun createEntityManagerFactory(dataSource: HikariDataSource, showSql: Boolean, formatSql: Boolean): EntityManagerFactory {
    logger.info("Creating YDB EntityManagerFactory programmatically")

    val configuration = Configuration()

    configuration.setProperty(AvailableSettings.DIALECT, "tech.ydb.hibernate.dialect.YdbDialect")
    configuration.setProperty(AvailableSettings.HBM2DDL_AUTO, "none")
    configuration.setProperty(AvailableSettings.SHOW_SQL, showSql)
    configuration.setProperty(AvailableSettings.FORMAT_SQL, formatSql)

    configuration.setProperty(AvailableSettings.STATEMENT_BATCH_SIZE, "0")
    configuration.setProperty(AvailableSettings.ORDER_INSERTS, "false")
    configuration.setProperty(AvailableSettings.ORDER_UPDATES, "false")

    configuration.setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "8")
    configuration.setProperty(AvailableSettings.USE_SQL_COMMENTS, "false")
    configuration.setProperty("hibernate.query.in_clause_parameter_padding", "true")
    configuration.setProperty(AvailableSettings.STATEMENT_FETCH_SIZE, "64")

    addKeycloakEntities(configuration)

    val serviceRegistry = StandardServiceRegistryBuilder()
      // TODO use not deprecated instead of DATASOURCE
      .applySetting(AvailableSettings.DATASOURCE, dataSource)
      .applySettings(configuration.properties)
      .build()

    val sessionFactory = configuration.buildSessionFactory(serviceRegistry)

    logger.info("YDB EntityManagerFactory created successfully")

    return sessionFactory.unwrap(EntityManagerFactory::class.java)
  }

  fun addKeycloakEntities(configuration: Configuration) {
    logger.debug("Adding Keycloak entity classes")

    val entityClasses = listOf(
      "org.keycloak.models.jpa.entities.ClientEntity",
      "org.keycloak.models.jpa.entities.ClientAttributeEntity",
      "org.keycloak.models.jpa.entities.CredentialEntity",
      "org.keycloak.models.jpa.entities.RealmEntity",
      "org.keycloak.models.jpa.entities.RealmAttributeEntity",
      "org.keycloak.models.jpa.entities.RequiredCredentialEntity",
      "org.keycloak.models.jpa.entities.ComponentConfigEntity",
      "org.keycloak.models.jpa.entities.ComponentEntity",
      "org.keycloak.models.jpa.entities.UserFederationProviderEntity",
      "org.keycloak.models.jpa.entities.UserFederationMapperEntity",
      "org.keycloak.models.jpa.entities.RoleEntity",
      "org.keycloak.models.jpa.entities.RoleAttributeEntity",
      "org.keycloak.models.jpa.entities.FederatedIdentityEntity",
      "org.keycloak.models.jpa.entities.MigrationModelEntity",
      "org.keycloak.models.jpa.entities.UserEntity",
      "org.keycloak.models.jpa.entities.RealmLocalizationTextsEntity",
      "org.keycloak.models.jpa.entities.UserRequiredActionEntity",
      "org.keycloak.models.jpa.entities.UserAttributeEntity",
      "org.keycloak.models.jpa.entities.UserRoleMappingEntity",
      "org.keycloak.models.jpa.entities.IdentityProviderEntity",
      "org.keycloak.models.jpa.entities.IdentityProviderMapperEntity",
      "org.keycloak.models.jpa.entities.ProtocolMapperEntity",
      "org.keycloak.models.jpa.entities.UserConsentEntity",
      "org.keycloak.models.jpa.entities.UserConsentClientScopeEntity",
      "org.keycloak.models.jpa.entities.AuthenticationFlowEntity",
      "org.keycloak.models.jpa.entities.AuthenticationExecutionEntity",
      "org.keycloak.models.jpa.entities.AuthenticatorConfigEntity",
      "org.keycloak.models.jpa.entities.RequiredActionProviderEntity",
      "org.keycloak.models.jpa.session.PersistentUserSessionEntity",
      "org.keycloak.models.jpa.session.PersistentClientSessionEntity",
      "org.keycloak.models.jpa.entities.RevokedTokenEntity",
      "org.keycloak.models.jpa.entities.GroupEntity",
      "org.keycloak.models.jpa.entities.GroupAttributeEntity",
      "org.keycloak.models.jpa.entities.GroupRoleMappingEntity",
      "org.keycloak.models.jpa.entities.UserGroupMembershipEntity",
      "org.keycloak.models.jpa.entities.ClientScopeEntity",
      "org.keycloak.models.jpa.entities.ClientScopeAttributeEntity",
      "org.keycloak.models.jpa.entities.ClientScopeRoleMappingEntity",
      "org.keycloak.models.jpa.entities.ClientScopeClientMappingEntity",
      "org.keycloak.models.jpa.entities.DefaultClientScopeRealmMappingEntity",
      "org.keycloak.models.jpa.entities.ClientInitialAccessEntity",

      // Events
      "org.keycloak.events.jpa.EventEntity",
      "org.keycloak.events.jpa.AdminEventEntity",

      // Authorization
      "org.keycloak.authorization.jpa.entities.ResourceServerEntity",
      "org.keycloak.authorization.jpa.entities.ResourceEntity",
      "org.keycloak.authorization.jpa.entities.ScopeEntity",
      "org.keycloak.authorization.jpa.entities.PolicyEntity",
      "org.keycloak.authorization.jpa.entities.PermissionTicketEntity",
      "org.keycloak.authorization.jpa.entities.ResourceAttributeEntity",

      // Federated storage
      "org.keycloak.storage.jpa.entity.BrokerLinkEntity",
      "org.keycloak.storage.jpa.entity.FederatedUser",
      "org.keycloak.storage.jpa.entity.FederatedUserAttributeEntity",
      "org.keycloak.storage.jpa.entity.FederatedUserConsentEntity",
      "org.keycloak.storage.jpa.entity.FederatedUserConsentClientScopeEntity",
      "org.keycloak.storage.jpa.entity.FederatedUserCredentialEntity",
      "org.keycloak.storage.jpa.entity.FederatedUserGroupMembershipEntity",
      "org.keycloak.storage.jpa.entity.FederatedUserRequiredActionEntity",
      "org.keycloak.storage.jpa.entity.FederatedUserRoleMappingEntity",

      // Organization
      "org.keycloak.models.jpa.entities.OrganizationEntity",
      "org.keycloak.models.jpa.entities.OrganizationDomainEntity",

      // Server config
      "org.keycloak.storage.configuration.jpa.entity.ServerConfigEntity",

      // Workflows
      "org.keycloak.models.workflow.WorkflowStateEntity"
    )

    var addedCount = 0
    var failedCount = 0

    entityClasses.forEach { className ->
      try {
        val clazz = Class.forName(className)
        configuration.addAnnotatedClass(clazz)
        addedCount++
      } catch (e: ClassNotFoundException) {
        logger.warn("Entity class not found: $className", e)
        failedCount++
      }
    }

    logger.info("Added $addedCount entity classes, $failedCount not found")
  }

}