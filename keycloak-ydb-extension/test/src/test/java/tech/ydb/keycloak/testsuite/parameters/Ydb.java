package tech.ydb.keycloak.testsuite.parameters;

import com.google.common.collect.ImmutableSet;
import org.jboss.logging.Logger;
import org.keycloak.authorization.jpa.store.JPAAuthorizationStoreFactory;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderSpi;
import org.keycloak.connections.jpa.DefaultJpaConnectionProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionSpi;
import org.keycloak.connections.jpa.updater.JpaUpdaterProviderFactory;
import org.keycloak.connections.jpa.updater.JpaUpdaterSpi;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionSpi;
import org.keycloak.connections.jpa.updater.liquibase.lock.LiquibaseDBLockProviderFactory;
import org.keycloak.events.jpa.JpaEventStoreProviderFactory;
import org.keycloak.migration.MigrationProviderFactory;
import org.keycloak.migration.MigrationSpi;
import org.keycloak.models.IdentityProviderStorageSpi;
import org.keycloak.models.dblock.DBLockSpi;
import org.keycloak.models.jpa.JpaGroupProviderFactory;
import org.keycloak.models.jpa.JpaIdentityProviderStorageProviderFactory;
import org.keycloak.models.jpa.JpaRoleProviderFactory;
import org.keycloak.models.jpa.JpaUserProviderFactory;
import org.keycloak.models.jpa.session.JpaRevokedTokensPersisterProviderFactory;
import org.keycloak.models.jpa.session.JpaUserSessionPersisterProviderFactory;
import org.keycloak.models.session.RevokedTokenPersisterSpi;
import org.keycloak.models.session.UserSessionPersisterSpi;
import org.keycloak.organization.OrganizationSpi;
import org.keycloak.organization.jpa.JpaOrganizationProviderFactory;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.LoginProtocolSpi;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.storage.DatastoreSpi;
import org.keycloak.storage.datastore.DefaultDatastoreProviderFactory;
import tech.ydb.keycloak.client.YdbClientProviderFactory;
import tech.ydb.keycloak.client.YdbClientScopeProviderFactory;
import tech.ydb.keycloak.connection.YdbConnectionProviderFactoryImpl;
import tech.ydb.keycloak.liquibase.YdbDBLockProviderFactory;
import tech.ydb.keycloak.liquibase.YdbLiquibaseConnectionProvider;
import tech.ydb.keycloak.realm.YdbRealmProviderFactory;
import tech.ydb.keycloak.testsuite.Config;
import tech.ydb.keycloak.testsuite.KeycloakModelParameters;
import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.YdbHelperFactory;

import java.util.Set;

import static tech.ydb.keycloak.config.ProviderConfig.PROVIDER_ID;

public class Ydb extends KeycloakModelParameters {

    private static final Logger LOG = Logger.getLogger(Ydb.class);

    static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
            .add(JpaConnectionSpi.class)
            .add(JpaUpdaterSpi.class)
            .add(LiquibaseConnectionSpi.class)
            .add(UserSessionPersisterSpi.class)
            .add(RevokedTokenPersisterSpi.class)
            .add(DatastoreSpi.class)
            .add(MigrationSpi.class)
            .add(LoginProtocolSpi.class)
            .add(DBLockSpi.class)
            .add(IdentityProviderStorageSpi.class)
            .add(IdentityProviderSpi.class)
            .add(OrganizationSpi.class)
            .build();

    /**
     * JPA providers used where YDB implementation is not yet available
     */
    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES =
            ImmutableSet.<Class<? extends ProviderFactory>>builder()
                    .add(DefaultDatastoreProviderFactory.class)
                    .add(YdbConnectionProviderFactoryImpl.class)
                    .add(DefaultJpaConnectionProviderFactory.class)
                    .add(JPAAuthorizationStoreFactory.class)
                    .add(YdbClientProviderFactory.class)
                    .add(YdbClientScopeProviderFactory.class)
                    .add(JpaEventStoreProviderFactory.class)
                    .add(JpaGroupProviderFactory.class)
                    .add(JpaIdentityProviderStorageProviderFactory.class)
                    .add(YdbRealmProviderFactory.class)
                    .add(JpaRoleProviderFactory.class)
                    .add(JpaUpdaterProviderFactory.class)
                    .add(JpaUserProviderFactory.class)
                    .add(YdbLiquibaseConnectionProvider.class)
                    .add(YdbDBLockProviderFactory.class)
                    .add(JpaUserSessionPersisterProviderFactory.class)
                    .add(JpaRevokedTokensPersisterProviderFactory.class)
                    .add(MigrationProviderFactory.class)
                    .add(LoginProtocolFactory.class)
                    .add(IdentityProviderFactory.class)
                    .add(JpaOrganizationProviderFactory.class)
                    .build();

    private YdbHelper ydbHelper;

    public Ydb() {
        super(ALLOWED_SPIS, ALLOWED_FACTORIES);
    }

    @Override
    public void beforeSuite(Config cf) {
        YdbHelperFactory factory = YdbHelperFactory.getInstance();
        if (!factory.isEnabled()) {
            LOG.warn("YDB helper is not available - tests will be skipped");
            return;
        }
        LOG.info("Creating YDB helper for Keycloak model tests");
        ydbHelper = factory.createHelper();
        if (ydbHelper == null) {
            LOG.warn("Failed to create YDB helper - tests will be skipped");
        }
    }

    @Override
    public void afterSuite() {
        if (ydbHelper != null) {
            try {
                ydbHelper.close();
            } catch (Exception e) {
                LOG.warnf(e, "Error closing YDB helper");
            }
            ydbHelper = null;
        }
    }

    @Override
    public void updateConfig(Config cf) {
        // Client and realm: YDB. Other SPIs below: JPA until YDB provider exists
        cf.spi("client").defaultProvider(PROVIDER_ID)
                .spi("clientScope").defaultProvider(PROVIDER_ID)
                .spi("realm").defaultProvider(PROVIDER_ID)
                .spi("connectionsLiquibase").defaultProvider(PROVIDER_ID)
                .spi("dblock").defaultProvider(PROVIDER_ID)
                .spi("group").defaultProvider("jpa")
                .spi("idp").defaultProvider("jpa")
                .spi("role").defaultProvider("jpa")
                .spi("user").defaultProvider("jpa")
                .spi("deploymentState").defaultProvider("jpa");

        // YDB connection - use ydb provider with jdbcUrl from YdbHelper
        if (ydbHelper != null) {
            String jdbcUrl = buildJdbcUrl(ydbHelper);
            cf.spi("connectionsJpa")
                    .defaultProvider(PROVIDER_ID)
                    .provider(PROVIDER_ID)
                    .config("jdbcUrl", jdbcUrl);
        }
    }

    private static String buildJdbcUrl(YdbHelper helper) {
        return "jdbc:ydb:" +
                (helper.useTls() ? "grpcs://" : "grpc://") +
                helper.endpoint() +
                helper.database();
    }
}
