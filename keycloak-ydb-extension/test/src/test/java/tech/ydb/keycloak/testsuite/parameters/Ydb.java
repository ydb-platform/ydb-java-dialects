/*
 * Copyright 2025 YDB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.ydb.keycloak.testsuite.parameters;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
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
import org.keycloak.models.UserSessionSpi;
import org.keycloak.models.dblock.DBLockSpi;
import org.keycloak.models.jpa.JpaClientProviderFactory;
import org.keycloak.models.jpa.JpaClientScopeProviderFactory;
import org.keycloak.models.jpa.JpaGroupProviderFactory;
import org.keycloak.models.jpa.JpaIdentityProviderStorageProviderFactory;
import org.keycloak.models.jpa.JpaRealmProviderFactory;
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
import tech.ydb.keycloak.config.ProviderConfig;
import tech.ydb.keycloak.connection.YdbConnectionProviderFactoryImpl;
import tech.ydb.keycloak.realm.YdbRealmProviderFactory;
import tech.ydb.keycloak.testsuite.Config;
import tech.ydb.keycloak.testsuite.KeycloakModelParameters;
import tech.ydb.test.integration.YdbHelper;
import tech.ydb.test.integration.YdbHelperFactory;

import static tech.ydb.keycloak.config.ProviderConfig.PROVIDER_ID;

/**
 * KeycloakModelParameters for YDB storage - configures JPA with YDB connection using YdbHelperExtension.
 */
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
                    .add(LiquibaseConnectionProviderFactory.class)
                    .add(LiquibaseDBLockProviderFactory.class)
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
        // JPA providers
        cf.spi("client").defaultProvider("ydb")
                .spi("clientScope").defaultProvider("ydb")
                .spi("group").defaultProvider("jpa")
                .spi("idp").defaultProvider("jpa")
                .spi("role").defaultProvider("jpa")
                .spi("user").defaultProvider("jpa")
                .spi("realm").defaultProvider("ydb")
                .spi("deploymentState").defaultProvider("jpa")
                .spi("dblock").defaultProvider("jpa");

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
