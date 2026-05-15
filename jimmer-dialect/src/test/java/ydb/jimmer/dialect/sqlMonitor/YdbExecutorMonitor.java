package ydb.jimmer.dialect.sqlMonitor;

import org.babyfish.jimmer.sql.runtime.AbstractExecutorProxy;
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class YdbExecutorMonitor extends AbstractExecutorProxy {
    private List<QueryLog> queryLogs = new ArrayList<>();

    public YdbExecutorMonitor(Executor raw) {
        super(raw);
    }

    public YdbExecutorMonitor(Executor raw, List<QueryLog> queryLogs) {
        super(raw);
        this.queryLogs = queryLogs;
    }

    public List<QueryLog> getLogs() {
        List<QueryLog> tmp = queryLogs;
        queryLogs = new ArrayList<>();
        return tmp;
    }

    @Override
    public <R> R execute(@NotNull Args<R> args) {
        queryLogs.add(QueryLog.simple(args.sql, args.purpose, args.variables));
        return raw.execute(args);
    }

    @Override
    protected AbstractExecutorProxy recreate(Executor raw) {
        return new YdbExecutorMonitor(raw, queryLogs);
    }

    @Override
    protected Batch createBatch(BatchContext raw) {
        return new YdbBatch(raw, queryLogs);
    }

    private static class YdbBatch extends AbstractExecutorProxy.Batch {
        private final List<QueryLog> queryLogs;
        private final List<List<Object>> variablesList = new ArrayList<>();

        protected YdbBatch(BatchContext raw, List<QueryLog> queryLogs) {
            super(raw);
            this.queryLogs = queryLogs;
        }

        @Override
        public void add(List<Object> variables) {
            raw.add(variables);
            variablesList.add(variables);
        }

        @Override
        public int[] execute(BiFunction<SQLException, ExceptionTranslator.Args, Exception> exceptionTranslator) {
            queryLogs.add(new QueryLog(raw.sql(), raw.purpose(), variablesList));
            return raw.execute(exceptionTranslator);
        }
    }
}
