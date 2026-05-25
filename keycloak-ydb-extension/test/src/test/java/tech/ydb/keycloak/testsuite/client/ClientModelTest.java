package tech.ydb.keycloak.testsuite.client;

import org.junit.Test;
import org.keycloak.models.*;
import tech.ydb.keycloak.testsuite.KeycloakModelTest;
import tech.ydb.keycloak.testsuite.RequireProvider;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RequireProvider(RealmProvider.class)
@RequireProvider(ClientProvider.class)
@RequireProvider(RoleProvider.class)
public class ClientModelTest extends KeycloakModelTest {

    private String realmId;

    private static final String searchClientId = "My ClIeNt WITH sP%Ces and sp*ci_l Ch***cters \" ?!";

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "realm");
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testClientsBasics() {
        // Create client
        ClientModel originalModel = withRealm(realmId, (session, realm) -> session.clients().addClient(realm, "myClientId"));
        ClientModel searchClient = withRealm(realmId, (session, realm) -> {
            ClientModel client = session.clients().addClient(realm, searchClientId);
            client.setAlwaysDisplayInConsole(true);
            client.addRedirectUri("http://www.redirecturi.com");
            return client;
        });
        assertThat(originalModel.getId(), notNullValue());

        // Find by id
        {
            ClientModel model = withRealm(realmId, (session, realm) -> session.clients().getClientById(realm, originalModel.getId()));
            assertThat(model, notNullValue());
            assertThat(model.getId(), is(equalTo(model.getId())));
            assertThat(model.getClientId(), is(equalTo("myClientId")));
        }

        // Find by clientId
        {
            ClientModel model = withRealm(realmId, (session, realm) -> session.clients().getClientByClientId(realm, "myClientId"));
            assertThat(model, notNullValue());
            assertThat(model.getId(), is(equalTo(originalModel.getId())));
            assertThat(model.getClientId(), is(equalTo("myClientId")));
        }

        // Search by clientId
        {
            withRealm(realmId, (session, realm) -> {
                ClientModel client = session.clients().searchClientsByClientIdStream(realm, "client with", 0, 10).findFirst().orElse(null);
                assertThat(client, notNullValue());
                assertThat(client.getId(), is(equalTo(searchClient.getId())));
                assertThat(client.getClientId(), is(equalTo(searchClientId)));
                return null;
            });


            withRealm(realmId, (session, realm) -> {
                ClientModel client = session.clients().searchClientsByClientIdStream(realm, "sp*ci_l Ch***cters", 0, 10).findFirst().orElse(null);
                assertThat(client, notNullValue());
                assertThat(client.getId(), is(equalTo(searchClient.getId())));
                assertThat(client.getClientId(), is(equalTo(searchClientId)));
                return null;
            });

            withRealm(realmId, (session, realm) -> {
                ClientModel client = session.clients().searchClientsByClientIdStream(realm, " AND ", 0, 10).findFirst().orElse(null);
                assertThat(client, notNullValue());
                assertThat(client.getId(), is(equalTo(searchClient.getId())));
                assertThat(client.getClientId(), is(equalTo(searchClientId)));
                return null;
            });

            withRealm(realmId, (session, realm) -> {
                // when searching by "%" all entries are expected
                assertThat(session.clients().searchClientsByClientIdStream(realm, "%", 0, 10).count(), is(equalTo(2L)));
                return null;
            });
        }

        // using Boolean operand
        {
            Map<ClientModel, Set<String>> allRedirectUrisOfEnabledClients = withRealm(realmId, (session, realm) -> session.clients().getAllRedirectUrisOfEnabledClients(realm));
            assertThat(allRedirectUrisOfEnabledClients.values(), hasSize(1));
            assertThat(allRedirectUrisOfEnabledClients.keySet().iterator().next().getId(), is(equalTo(searchClient.getId())));
        }

        // Test storing flow binding override
        {
            // Add some override
            withRealm(realmId, (session, realm) -> {
                ClientModel clientById = session.clients().getClientById(realm, originalModel.getId());
                clientById.setAuthenticationFlowBindingOverride("browser", "customFlowId");
                return clientById;
            });

            String browser = withRealm(realmId, (session, realm) -> session.clients().getClientById(realm, originalModel.getId()).getAuthenticationFlowBindingOverride("browser"));
            assertThat(browser, is(equalTo("customFlowId")));
        }
    }

    @Test
    public void testScopeMappingRoleRemoval() {
        // create two clients, one realm role and one client role and assign both to one of the clients
        inComittedTransaction(1, (session , i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            ClientModel client1 = session.clients().addClient(realm, "client1");
            ClientModel client2 = session.clients().addClient(realm, "client2");
            RoleModel realmRole = session.roles().addRealmRole(realm, "realm-role");
            RoleModel client2Role = session.roles().addClientRole(client2, "client2-role");
            client1.addScopeMapping(realmRole);
            client1.addScopeMapping(client2Role);
            return null;
        });

        // check everything is OK
        inComittedTransaction(1, (session, i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            final ClientModel client1 = session.clients().getClientByClientId(realm, "client1");
            assertThat(client1.getScopeMappingsStream().count(), is(2L));
            assertThat(client1.getScopeMappingsStream().filter(r -> r.getName().equals("realm-role")).count(), is(1L));
            assertThat(client1.getScopeMappingsStream().filter(r -> r.getName().equals("client2-role")).count(), is(1L));
            return null;
        });

        // remove the realm role
        inComittedTransaction(1, (session, i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            final RoleModel role = session.roles().getRealmRole(realm, "realm-role");
            session.roles().removeRole(role);
            return null;
        });

        // check it is removed
        inComittedTransaction(1, (session, i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            final ClientModel client1 = session.clients().getClientByClientId(realm, "client1");
            assertThat(client1.getScopeMappingsStream().count(), is(1L));
            assertThat(client1.getScopeMappingsStream().filter(r -> r.getName().equals("client2-role")).count(), is(1L));
            return null;
        });

        // remove client role
        inComittedTransaction(1, (session, i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            final ClientModel client2 = session.clients().getClientByClientId(realm, "client2");
            final RoleModel role = session.roles().getClientRole(client2, "client2-role");
            session.roles().removeRole(role);
            return null;
        });

        // check both clients are removed
        inComittedTransaction(1, (session, i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            final ClientModel client1 = session.clients().getClientByClientId(realm, "client1");
            assertThat(client1.getScopeMappingsStream().count(), is(0L));
            return null;
        });

        // remove clients
        inComittedTransaction(1, (session , i) -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            final ClientModel client1 = session.clients().getClientByClientId(realm, "client1");
            final ClientModel client2 = session.clients().getClientByClientId(realm, "client2");
            session.clients().removeClient(realm, client1.getId());
            session.clients().removeClient(realm, client2.getId());
            return null;
        });
    }
}
