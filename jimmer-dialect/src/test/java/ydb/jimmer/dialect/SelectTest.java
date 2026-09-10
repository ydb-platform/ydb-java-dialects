package ydb.jimmer.dialect;

import org.junit.jupiter.api.Test;
import ydb.jimmer.dialect.model.StudentTable;

public class SelectTest extends AbstractSelectTest {
    @Test
    public void OneEntityTest() {
        initDatabase();

        StudentTable table = StudentTable.$;

        executeAndExpect(
                getYqlClient()
                        .createQuery(table)
                        .select(table),
                cxt -> cxt.sql(
                        "select tb_1_.id, tb_1_.name, tb_1_.group " +
                                "from student tb_1_"
                )
        );
    }
}
