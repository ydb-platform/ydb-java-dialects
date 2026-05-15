package ydb.jimmer.dialect.versioning;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.junit.jupiter.api.Test;
import ydb.jimmer.dialect.AbstractInsertTest;
import ydb.jimmer.dialect.model.versioning.BookStoreDraft;

public class InsertVersionTest extends AbstractInsertTest {
    private static final String TABLE_NAME = "version_table";
    private static final String TYPE_NAME = "Int32";

    @Test
    public void simpleTest() {
        createTable(TABLE_NAME, TYPE_NAME);

        getIsolationClient().getEntities().saveCommand(
                BookStoreDraft.$.produce(version ->
                        version.setId(0).setValue(0)
                )
        ).setMode(SaveMode.INSERT_ONLY).execute();

        executeAndExpect(
                getYqlClient().getEntities().saveCommand(
                        BookStoreDraft.$.produce(version ->
                                version.setId(0).setValue(2)
                        )
                ).setMode(SaveMode.UPDATE_ONLY),
                ctx -> {
                    ctx.nextStatement();
                    ctx.sql("UPDATE " + TABLE_NAME + " set value = value + 1 where id = ? and value = ? returning id");
                    ctx.error("Save error caused by the path: \"<root>\": " +
                            "Cannot update the entity whose type is " +
                            "\"ydb.jimmer.dialect.model.versioning.BookStore\" " +
                            "and id is \"0\" because of optimistic lock error");
                }
        );

        dropTable(TABLE_NAME);
    }
}
