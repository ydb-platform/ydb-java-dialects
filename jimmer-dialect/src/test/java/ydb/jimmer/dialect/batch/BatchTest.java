package ydb.jimmer.dialect.batch;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ydb.jimmer.dialect.AbstractInsertTest;
import ydb.jimmer.dialect.model.Entity;
import ydb.jimmer.dialect.model.EntityDraft;

import java.util.ArrayList;
import java.util.List;

public class BatchTest  extends AbstractInsertTest {
    private static final String TABLE_NAME = "simple_table";

    private void batchTest(
            SaveMode saveMode,
            String sql,
            int[][] batches,
            int[][] expectedBatches
    ) {
        List<Entity> entities = new ArrayList<>();
        for (int[] batch : batches) {
            entities.add(EntityDraft.$.produce(t -> {
                t.setId(batch[0]);
                t.setValue(batch[1]);
            }));
        }

        executeAndExpect(
                getYqlClientForBatch()
                        .getEntities()
                        .saveEntitiesCommand(entities)
                        .setMode(saveMode),
                cxt -> {
                    cxt.sql(sql);

                    for (int i = 0; i < batches.length; i++) {
                        cxt.batchVariables(i, expectedBatches[i][0], expectedBatches[i][1]);
                    }
                }
        );
    }

    @BeforeEach
    void setup() {
        createTable(TABLE_NAME, "Int32");
    }

    @AfterEach
    void teardown() {
        dropTable(TABLE_NAME);
    }

    @Test
    public void insertTest() {
        int[][] batches = new int[][]{{0, 123}, {1, 456}};
        batchTest(SaveMode.INSERT_ONLY,
                "insert into " + TABLE_NAME + "(id, value) values(?, ?)",
                batches, batches);
    }

    @Test
    public void updateTest() {
        int[][] batches = new int[][]{{0, 123}, {1, 456}};
        int[][] expectedBatches = new int[][]{{123, 0}, {456, 1}};
        batchTest(SaveMode.UPDATE_ONLY,
                "update " + TABLE_NAME + " set value = ? where id = ? returning id",
                batches, expectedBatches);
    }

    @Test
    public void upsertTest() {
        int[][] batches = new int[][]{{0, 123}, {1, 456}};
        batchTest(SaveMode.UPSERT,
                "upsert into " + TABLE_NAME + "(id, value) values(?, ?)",
                batches, batches);
    }
}
