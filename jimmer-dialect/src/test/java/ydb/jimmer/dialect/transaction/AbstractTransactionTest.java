package ydb.jimmer.dialect.transaction;

import org.babyfish.jimmer.sql.ast.mutation.MutationResult;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import ydb.jimmer.dialect.AbstractSelectTest;
import ydb.jimmer.dialect.QueryTestContext;
import ydb.jimmer.dialect.model.transaction.YdbTransaction;
import ydb.jimmer.dialect.model.transaction.YdbTransactionDraft;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractTransactionTest extends AbstractSelectTest {
    private static final String TABLE_NAME = "ydb_transaction";
    private static final String TYPE_NAME = "Int32";

    protected static final YqlClient yqlClient = getIsolationClient();

    protected void readTest(Function<Supplier<List<YdbTransaction>>, List<YdbTransaction>> transaction) {
        String[] values = new String[]{"-1", "0", "10"};

        createTable(TABLE_NAME, TYPE_NAME);

        insert(TABLE_NAME, values);

        List<YdbTransaction> rows = transaction.apply(() ->
                yqlClient.getEntities().findAll(YdbTransaction.class)
        );
        QueryTestContext cxt = new QueryTestContext(executor.getLogs(), rows);

        cxt.sql("select tb_1_.id, tb_1_.value from " + TABLE_NAME + " tb_1_");

        String json = buildJsonResponse(values);
        cxt.rows(json);

        dropTable(TABLE_NAME);
    }

    protected void writeTest(Function<Supplier<MutationResult>, MutationResult> transaction, boolean readOnly) {
        String errorMessage = null;
        if (readOnly) {
            errorMessage = "Cannot execute the DML statement";
        }

        writeTest(transaction, errorMessage);
    }

    protected void writeTest(Function<Supplier<MutationResult>, MutationResult> transaction, String errorMessage) {
        Object[] variables = new Object[]{0, 10};

        createTable(TABLE_NAME, TYPE_NAME);

        MutationResult result = null;
        Throwable throwable = null;
        try {
            result = transaction.apply(() ->
                    yqlClient.getEntities().saveCommand(
                            YdbTransactionDraft.$.produce(item -> {
                                item.setId(0);
                                item.setValue(10);
                            })
                    ).setMode(SaveMode.INSERT_ONLY).execute()
            );
        } catch (Throwable ex) {
            throwable = ex;
        }
        QueryTestContext cxt = new QueryTestContext(executor.getLogs(), result, throwable);

        cxt.sql("insert into " + TABLE_NAME + "(id, value) values(?, ?)");
        cxt.variables(variables);
        cxt.error(errorMessage);

        dropTable(TABLE_NAME);
    }
}
