package ydb.jimmer.dialect.sqlMonitor;

import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;

import java.util.Collections;
import java.util.List;

public class QueryLog {
    private final String sql;
    private final ExecutionPurpose purpose;
    private final List<List<Object>> variablesList;

    public QueryLog(String sql, ExecutionPurpose purpose, List<List<Object>> variablesList) {
        this.sql = sql;
        this.purpose = purpose;
        this.variablesList = variablesList;
    }

    public static QueryLog simple(String sql, ExecutionPurpose purpose, List<Object> variables) {
        return new QueryLog(sql, purpose, Collections.singletonList(variables));
    }

    public String getSql() {
        return sql;
    }

    public ExecutionPurpose getPurpose() {
        return purpose;
    }

    public List<List<Object>> getVariablesList() {
        return variablesList;
    }
}
