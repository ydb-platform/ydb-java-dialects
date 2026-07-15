package ydb.jimmer.dialect.streaming;

import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.junit.jupiter.api.Test;
import ydb.jimmer.dialect.AbstractSelectTest;
import ydb.jimmer.dialect.model.streaming.YdbStreamingTable;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class StreamingTest extends AbstractSelectTest {
    private static final String TABLE_NAME = "ydb_streaming";
    private static final String TYPE_NAME = "Int32";

    @Test
    public void cursorTest() {
        AbstractTypedTable<?> table = YdbStreamingTable.$;
        PropExpression<?> prop = YdbStreamingTable.$.value();
        String[] values = new String[]{"11", "12", "21", "22"};

        createTable(TABLE_NAME, TYPE_NAME);

        insert(TABLE_NAME, values);

        String json = buildJsonResponse(values);

        executeAndExpect((Connection con) -> {
                    List<Object> responses = new ArrayList<>();
                    getYqlClient()
                            .createQuery(table)
                            .orderBy(prop)
                            .select(table)
                            .forEach(con, 2, responses::add);
                    return responses;
                },
                cxt -> {
                    cxt.sql(
                            "select tb_1_.id, tb_1_.value from " + TABLE_NAME + " tb_1_ order by tb_1_.value asc");
                    cxt.rows(json);
                }
        );
    }
}
