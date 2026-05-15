package ydb.jimmer.dialect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.sql.ast.mutation.MutationResult;
import org.junit.jupiter.api.Assertions;
import ydb.jimmer.dialect.sqlMonitor.QueryLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QueryTestContext {
    private static final ObjectMapper MAPPER = new ObjectMapper()
                    .registerModule(new ImmutableModule())
                    .registerModule(new JavaTimeModule());

    static {
        MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
    }

    private final List<QueryLog> logs;
    private final List<?> rows;
    private final MutationResult result;
    private final Throwable throwable;

    private int index = 0;

    public QueryTestContext(List<QueryLog> logs, List<?> rows) {
        this.logs = logs;
        this.rows = rows;
        result = null;
        throwable = null;
    }

    public QueryTestContext(List<QueryLog> logs, MutationResult result, Throwable throwable) {
        this.logs = logs;
        this.rows = new ArrayList<>();
        this.result = result;
        this.throwable = throwable;
    }

    public void nextStatement() {
        index++;
    }

    public void sql(String sql) {
        Assertions.assertEquals(
                sql.replace("--->", "").toLowerCase(),
                logs.get(index).getSql().toLowerCase(),
                "statements[" + index + "].sql");
    }

    public void variables(Object... values) {
        batchVariables(0, values);
        batches(1);
    }

    public void batches(int batchCount) {
        Assertions.assertEquals(batchCount, logs.get(index).getVariablesList().size());
    }

    public void batchVariables(int batchIndex, Object... values) {
        Assertions.assertEquals(
                values.length,
                logs.get(index).getVariablesList().get(batchIndex).size(),
                "statements[" + index + "].variables.size is error, actual variables: " +
                        logs.get(index).getVariablesList().get(batchIndex)
        );
        for (int i = 0; i < values.length; i++) {
            Object expect = values[i];

            Object actual = logs.get(index).getVariablesList().get(batchIndex).get(i);

            Assertions.assertEquals(
                    expect,
                    actual,
                    "statements[" + index + "].batch[" + batchIndex + "].variables[" + i + "] is error, " +
                            "expected variables: " +
                            Arrays.toString(values) +
                            ", actual variables: " +
                            actual
            );
        }
    }

    public void rows(String json) {
        try {
            Assertions.assertEquals(
                    json.replace("--->", ""),
                    MAPPER.writeValueAsString(rows)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void error(String message) {
        if (throwable == null && message == null) {
            return;
        }

        if (throwable == null) {
            Assertions.fail("The query didn't produce an exception: " + message);
        }

        Assertions.assertEquals(message, throwable.getMessage());
    }
}
