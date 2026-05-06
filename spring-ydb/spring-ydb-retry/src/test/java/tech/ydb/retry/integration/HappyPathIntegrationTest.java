package tech.ydb.retry.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tech.ydb.retry.integration.app.User;
import tech.ydb.retry.integration.app.UserApplication;
import tech.ydb.retry.integration.app.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = UserApplication.class)
@ActiveProfiles({"enabled", "ydb"})
class HappyPathIntegrationTest extends YdbDockerTest {

    @Autowired
    private UserService userService;

    @BeforeEach
    void cleanUp() {
        userService.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        User user = createUser(1L, "user1", "first", "last");
        userService.save(user);

        User found = userService.findById(1L);
        assertNotNull(found);
        assertEquals("user1", found.getUsername());
        assertEquals("first", found.getFirstname());
        assertEquals("last", found.getLastname());
    }

    @Test
    void shouldSaveRaw() {
        User user = createUser(2L, "user2", "first", "last");
        userService.saveRaw(user);

        User found = userService.findById(2L);
        assertNotNull(found);
        assertEquals("user2", found.getUsername());
    }

    @Test
    void shouldSaveWithMaxRetries3() {
        User user = createUser(3L, "user3", "first", "last");
        userService.saveWithMaxRetries3(user);

        User found = userService.findById(3L);
        assertNotNull(found);
        assertEquals("user3", found.getUsername());
    }

    @Test
    void shouldSaveIdempotent() {
        User user = createUser(4L, "user4", "first", "last");
        userService.saveIdempotent(user);

        User found = userService.findById(4L);
        assertNotNull(found);
        assertEquals("user4", found.getUsername());
    }

    @Test
    void shouldUpdateFirstname() {
        userService.save(createUser(5L, "user5", "original", "last"));

        userService.updateFirstname(5L, "updated");
        User found = userService.findById(5L);
        assertNotNull(found);
        assertEquals("updated", found.getFirstname());
    }

    @Test
    void shouldDeleteAll() {
        userService.save(createUser(6L, "user6", "first", "last"));
        userService.save(createUser(7L, "user7", "first", "last"));

        userService.deleteAll();

        assertNull(userService.findById(6L));
        assertNull(userService.findById(7L));
    }

    @Test
    void shouldReturnNullForNonExistentUser() {
        assertNull(userService.findById(999L));
    }

    private User createUser(Long id, String username, String firstname, String lastname) {
        return new User(id, username, firstname, lastname);
    }
}
