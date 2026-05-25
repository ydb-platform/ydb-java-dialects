
package tech.ydb.keycloak.testsuite.clientscope;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.models.*;
import org.keycloak.models.cache.CacheRealmProvider;
import tech.ydb.keycloak.testsuite.KeycloakModelTest;
import tech.ydb.keycloak.testsuite.RequireProvider;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RequireProvider(RealmProvider.class)
@RequireProvider(ClientProvider.class)
@RequireProvider(ClientScopeProvider.class)
@RequireProvider(RoleProvider.class)
public class ClientScopeModelTest extends KeycloakModelTest {

    private String realmId;

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
    public void testClientScopes() {
        List<String> clientScopes = new LinkedList<>();
        withRealm(realmId, (session, realm) -> {
            ClientModel client = session.clients().addClient(realm, "myClientId");

            ClientScopeModel clientScope1 = session.clientScopes().addClientScope(realm, "myClientScope1");
            clientScopes.add(clientScope1.getId());
            ClientScopeModel clientScope2 = session.clientScopes().addClientScope(realm, "myClientScope2");
            clientScopes.add(clientScope2.getId());


            client.addClientScope(clientScope1, true);
            client.addClientScope(clientScope2, false);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            List<String> actualClientScopes = session.clientScopes().getClientScopesStream(realm).map(ClientScopeModel::getId).collect(Collectors.toList());
            assertThat(actualClientScopes, containsInAnyOrder(clientScopes.toArray()));

            ClientScopeModel clientScopeById = session.clientScopes().getClientScopeById(realm, clientScopes.get(0));
            assertThat(clientScopeById.getId(), is(clientScopes.get(0)));

            session.clientScopes().removeClientScopes(realm);

            return null;
        });

        withRealm(realmId, (session, realm) -> {
            List<ClientScopeModel> actualClientScopes = session.clientScopes().getClientScopesStream(realm).collect(Collectors.toList());
            assertThat(actualClientScopes, empty());

            return null;
        });
    }

    @Test
    @RequireProvider(value=ClientScopeProvider.class, only="ydb")
    @RequireProvider(value=CacheRealmProvider.class)
    public void testClientScopesCaching() {
        List<String> clientScopes = new LinkedList<>();
        withRealm(realmId, (session, realm) -> {
            ClientScopeModel clientScope = session.clientScopes().addClientScope(realm, "myClientScopeForCaching");
            clientScopes.add(clientScope.getId());

            assertionsForClientScopesCaching(clientScopes, session, realm);
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            assertionsForClientScopesCaching(clientScopes, session, realm);
            return null;
        });

    }

    private static void assertionsForClientScopesCaching(List<String> clientScopes, KeycloakSession session, RealmModel realm) {
        assertThat(clientScopes, Matchers.containsInAnyOrder(realm.getClientScopesStream()
                .map(ClientScopeModel::getId).toArray(String[]::new)));

        assertThat(clientScopes, Matchers.containsInAnyOrder(session.clientScopes().getClientScopesStream(realm)
                .map(ClientScopeModel::getId).toArray(String[]::new)));
    }

}
