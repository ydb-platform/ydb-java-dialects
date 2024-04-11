package tech.ydb.flyway.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * @author Kirill Kurdyukov
 */
public class YdbFlywayInfoTest extends YdbFlywayBaseTest {

    @Test
    void simpleTest() {
        createFlyway("classpath:db/migration-step-3").load().migrate();

        var flyway = createFlyway("classpath:db/migration").load();

        var info = flyway.info();

        assertEquals(3, info.applied().length);
        assertEquals(6, info.all().length);

        flyway.migrate();
    }
}
