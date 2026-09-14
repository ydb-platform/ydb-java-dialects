package ydb.jimmer.dialect.dataTypeTest;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.junit.jupiter.api.Test;
import ydb.jimmer.dialect.AbstractSelectTest;
import ydb.jimmer.dialect.model.type.ydbJson.Json;
import ydb.jimmer.dialect.model.type.ydbJson.YdbJsonDraft;
import ydb.jimmer.dialect.model.type.ydbJson.YdbJsonTable;

public class InsertJsonTest extends AbstractSelectTest {
    private static final String TABLE_NAME = "ydb_json";
    private static final String TYPE_NAME = "Json";

    @Test
    public void insertJsonTest() {
        Json json = new Json();
        json.setA(2);
        json.setB(3);

        Object[] variables = new Object[]{0, json};

        String[] expectedValues = new String[]{"{\"a\":2,\"b\":3}"};
        String expectedJson = buildJsonResponse(expectedValues);

        YdbJsonTable table = YdbJsonTable.$;

        createTable(TABLE_NAME, TYPE_NAME);

        getIsolationClient().getEntities().saveCommand(YdbJsonDraft.$.produce(t -> {
                    t.setId((Integer) variables[0]);
                    t.setValue((Json) variables[1]);
                }))
                .setMode(SaveMode.INSERT_ONLY).execute();
        executeAndExpect(getYqlClient().createQuery(table).select(table),
                cxt -> {
                    cxt.sql("insert into " + TABLE_NAME + "(id, value) values(?, ?)");
                    cxt.nextStatement();
                    cxt.sql("select tb_1_.id, tb_1_.value from " + TABLE_NAME + " tb_1_");
                    cxt.rows(expectedJson);
                }
        );

        dropTable(TABLE_NAME);
    }
}
