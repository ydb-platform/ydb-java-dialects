package tech.ydb.keycloak.testsuite.session;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import tech.ydb.keycloak.testsuite.KeycloakModelTest;
import tech.ydb.keycloak.testsuite.RequireProvider;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static tech.ydb.keycloak.testsuite.session.UserSessionPersisterProviderTest.createClients;
import static tech.ydb.keycloak.testsuite.session.UserSessionPersisterProviderTest.createSessions;

@RequireProvider(UserSessionProvider.class)
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
public class UserSessionProviderModelTest extends KeycloakModelTest {

    private String realmId;
    private KeycloakSession kcSession;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "test");
        s.getContext().setRealm(realm);
        realm.setOfflineSessionIdleTimeout(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        realm.setSsoSessionIdleTimeout(1800);
        realm.setSsoSessionMaxLifespan(36000);
        realm.setClientSessionIdleTimeout(500);
        this.realmId = realm.getId();
        this.kcSession = s;

        s.users().addUser(realm, "user1").setEmail("user1@localhost");
        s.users().addUser(realm, "user2").setEmail("user2@localhost");

        createClients(s, realm);
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testMultipleSessionsRemovalInOneTransaction() {
        UserSessionModel[] origSessions = inComittedTransaction(session -> { return createSessions(session, realmId); });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);

            UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[0].getId());
            Assert.assertEquals(origSessions[0], userSession);

            userSession = session.sessions().getUserSession(realm, origSessions[1].getId());
            Assert.assertEquals(origSessions[1], userSession);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);

            session.sessions().removeUserSession(realm, session.sessions().getUserSession(realm, origSessions[0].getId()));
            session.sessions().removeUserSession(realm, session.sessions().getUserSession(realm, origSessions[1].getId()));
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);

            UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[0].getId());
            Assert.assertNull(userSession);

            userSession = session.sessions().getUserSession(realm, origSessions[1].getId());
            Assert.assertNull(userSession);
        });
    }

    @Test
    public void testTransientUserSessionIsNotPersisted() {
        String id = inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            UserSessionModel userSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT);

            ClientModel testApp = realm.getClientByClientId("test-app");
            session.sessions().createClientSession(realm, testApp, userSession);

            // assert the client sessions are present
            assertThat(session.sessions().getClientSession(userSession, testApp, false), notNullValue());
            return userSession.getId();
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            UserSessionModel userSession = session.sessions().getUserSession(realm, id);

            // in new transaction transient session should not be present
            assertThat(userSession, nullValue());
        });
    }

    @Test
    public void testClientSessionIsNotPersistedForTransientUserSession() {
        UserSessionModel userSession = inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            UserSessionModel us = session.sessions().createUserSession(null, realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT);
            ClientModel testApp = realm.getClientByClientId("test-app");
            session.sessions().createClientSession(realm, testApp, us);

            // assert the client sessions are present
            assertThat(session.sessions().getClientSession(us, testApp, false), notNullValue());
            return us;
        });
        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            ClientModel testApp = realm.getClientByClientId("test-app");
            // in new transaction transient session should not be present
            assertThat(session.sessions().getClientSession(userSession, testApp, false), nullValue());
        });
    }

    @Test
    public void testCreateUserSessionsParallel() throws InterruptedException {
        Set<String> userSessionIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        CountDownLatch latch = new CountDownLatch(4);

        inIndependentFactories(4, 30, () -> {
            withRealm(realmId, (session, realm) -> {
                UserModel user = session.users().getUserByUsername(realm, "user1");
                UserSessionModel userSession = session.sessions().createUserSession(null, realm, user, "user1", "", "", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                userSessionIds.add(userSession.getId());

                latch.countDown();

                return null;
            });

            // wait for other nodes to finish
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertThat(userSessionIds, Matchers.iterableWithSize(4));

            // wait a bit to allow replication
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            withRealm(realmId, (session, realm) -> {
                userSessionIds.forEach(id -> Assert.assertNotNull(session.sessions().getUserSession(realm, id)));

                return null;
            });
        });
    }

    @Test
    public void testStreamsMarshalling() throws InterruptedException {
        Assume.assumeTrue(InfinispanUtils.isEmbeddedInfinispan());
        closeKeycloakSessionFactory();
        var clusterSize = 4;
        var barrier = new CyclicBarrier(clusterSize);

        inIndependentFactories(clusterSize, 30, () -> {
            // populate the cache
            withRealmConsumer(realmId, (keycloakSession, realm) -> {
                var user = keycloakSession.users().getUserByUsername(realm, "user1");
                var client = realm.getClientByClientId("test-app");
                assertNotNull(user);
                assertNotNull(client);
                var userSession = keycloakSession.sessions().createUserSession(null, realm, user,  "user1", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                assertNotNull(userSession);
                var clientSession = keycloakSession.sessions().createClientSession(realm, client, userSession);
                assertNotNull(clientSession);
            });

            try {
                barrier.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (BrokenBarrierException | TimeoutException e) {
                throw new RuntimeException(e);
            }

            withRealmConsumer(realmId, (keycloakSession, realm) -> {
                var user = keycloakSession.users().getUserByUsername(realm, "user1");
                assertNotNull(user);

                var client = realm.getClientByClientId("test-app");
                assertNotNull(client);

                var activeClientSessionsStats = keycloakSession.sessions().getActiveClientSessionStats(realm, false);
                assertNotNull(activeClientSessionsStats);
                assertEquals(1, activeClientSessionsStats.size());
                assertTrue(activeClientSessionsStats.containsKey(client.getId()));
                assertEquals(4L, (long) activeClientSessionsStats.get(client.getId()));

                var userSessions = keycloakSession.sessions().getUserSessionsStream(realm, user).toList();
                assertNotNull(userSessions);
                assertEquals(4, userSessions.size());

                // sync everybody here since we are going to remove everything.
                try {
                    barrier.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (BrokenBarrierException | TimeoutException e) {
                    throw new RuntimeException(e);
                }

                keycloakSession.sessions().removeUserSessions(realm, user);
            });
        });
    }
}
