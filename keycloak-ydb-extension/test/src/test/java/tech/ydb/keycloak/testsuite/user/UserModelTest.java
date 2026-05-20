package tech.ydb.keycloak.testsuite.user;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.models.*;
import tech.ydb.keycloak.testsuite.KeycloakModelTest;
import tech.ydb.keycloak.testsuite.RequireProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
public class UserModelTest extends KeycloakModelTest {

    protected static final int NUM_GROUPS = 100;

    private String realmId;
    private final List<String> groupIds = new ArrayList<>(NUM_GROUPS);

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "realm");
        s.getContext().setRealm(realm);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();

        IntStream.range(0, NUM_GROUPS).forEach(i -> {
            groupIds.add(s.groups().createGroup(realm, "group-" + i).getId());
        });
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Override
    protected boolean isUseSameKeycloakSessionFactoryForAllThreads() {
        return true;
    }

    private Void addRemoveUser(KeycloakSession session, int i) {
        RealmModel realm = session.realms().getRealmByName("realm");
        session.getContext().setRealm(realm);

        UserModel user = session.users().addUser(realm, "user-" + i);

        IntStream.range(0, NUM_GROUPS / 20).forEach(gIndex -> {
            user.joinGroup(session.groups().getGroupById(realm, groupIds.get((i + gIndex) % NUM_GROUPS)));
        });

        final UserModel obtainedUser = session.users().getUserById(realm, user.getId());

        assertThat(obtainedUser, Matchers.notNullValue());
        assertThat(obtainedUser.getUsername(), is("user-" + i));
        Set<String> userGroupIds = obtainedUser.getGroupsStream().map(GroupModel::getName).collect(Collectors.toSet());
        assertThat(userGroupIds, hasSize(NUM_GROUPS / 20));
        assertThat(userGroupIds, hasItem("group-" + i));
        assertThat(userGroupIds, hasItem("group-" + (i - 1 + (NUM_GROUPS / 20)) % NUM_GROUPS));

        assertTrue(session.users().removeUser(realm, user));
        assertFalse(session.users().removeUser(realm, user));
        assertNull(session.users().getUserByUsername(realm, user.getUsername()));

        return null;
    }

    @Test
    public void testAddRemoveUser() {
        inRolledBackTransaction(1, this::addRemoveUser);
    }
}
