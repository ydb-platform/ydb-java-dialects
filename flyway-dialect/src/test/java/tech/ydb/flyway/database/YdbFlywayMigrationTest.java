package tech.ydb.flyway.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * @author Kirill Kurdyukov
 */
public class YdbFlywayMigrationTest extends YdbFlywayBaseTest {

    private static final String[] EVOLUTION_SCHEMA_MIGRATION_DIRS = new String[]{
            "migration-step-1", "migration-step-2",
            "migration-step-3", "migration-step-4",
            "migration-step-5", "migration",
    };

    private static final Set<String> EXPECTED_SCRIPTS = Set.of(
            "V1__create_series.sql", "V2__create_seasons.sql",
            "V3__create_episodes.sql", "V4__load_data.sql",
            "V5__create_series_title_index.sql", "V6__rename_index_title_index.sql"
    );

    @Test
    void simpleTest() {
        assertTrue(createFlyway("classpath:db/migration").load().migrate().success);
    }

    @Test
    void evolutionSchemaTest() {
        for (String migrationDir : EVOLUTION_SCHEMA_MIGRATION_DIRS) {
            assertTrue(createFlyway("classpath:db/" + migrationDir).load().migrate().success);
        }
    }

    @Test
    void evolutionConcurrencySchemaTest() throws ExecutionException, InterruptedException {
        int threadPoolSize = 10;

        ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);

        for (int migrationStep = 0; migrationStep < EVOLUTION_SCHEMA_MIGRATION_DIRS.length; migrationStep++) {
            List<Future<?>> taskFutures = new ArrayList<>();

            for (int i = 0; i < threadPoolSize * 2; i++) {
                int finalMigrationStep = migrationStep;

                taskFutures.add(
                        threadPool.submit(() -> assertTrue(
                                createFlyway("classpath:db/" + EVOLUTION_SCHEMA_MIGRATION_DIRS[finalMigrationStep])
                                        .load().migrate().success
                        ))
                );
            }

            for (Future<?> taskFuture : taskFutures) {
                taskFuture.get();
            }
        }
    }

    @Override
    protected Set<String> expectedScripts() {
        return EXPECTED_SCRIPTS;
    }
}
