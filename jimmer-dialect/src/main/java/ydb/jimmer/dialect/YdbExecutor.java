package ydb.jimmer.dialect;

import org.babyfish.jimmer.sql.runtime.AbstractExecutorProxy;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.SqlFunction;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Takes the ResultSet returned by the "UPDATE" operator with "RETURNING"
 * and returns the row count of the affected rows.
 * By default, Jimmer does not support ResultSet for "UPDATE".
 */
public class YdbExecutor extends AbstractExecutorProxy {
    public YdbExecutor(Executor raw) {
        super(raw);
    }

    @Override
    public <R> R execute(@NotNull Args<R> args) {
        if (args.purpose != ExecutionPurpose.MUTATE) {
            return raw.execute(args);
        }

        SqlFunction<PreparedStatement, R> originalBlock = args.block;
        Args<R> newArgs = new Args<>(
                args.sqlClient,
                args.con,
                args.sql,
                args.variables,
                args.variablePositions,
                args.purpose,
                args.getExceptionTranslator(),
                args.statementFactory,
                (stmt, a) -> originalBlock.apply(wrapStatement(stmt), a)
        );
        return raw.execute(newArgs);
    }

    private PreparedStatement wrapStatement(PreparedStatement stmt) {
        return (PreparedStatement) Proxy.newProxyInstance(
                stmt.getClass().getClassLoader(),
                new Class[]{PreparedStatement.class},
                (proxy, method, methodArgs) -> {
                    if ("executeUpdate".equals(method.getName()) && (methodArgs == null || methodArgs.length == 0)) {
                        boolean hasResultSet = stmt.execute();
                        if (hasResultSet) {
                            try (ResultSet rs = stmt.getResultSet()) {
                                int count = 0;
                                while (rs.next()) {
                                    count++;
                                }
                                return count;
                            }
                        }

                        return Math.max(stmt.getUpdateCount(), 0);
                    }

                    return method.invoke(stmt, methodArgs);
                }
        );
    }

    @Override
    protected AbstractExecutorProxy recreate(Executor raw) {
        return new YdbExecutor(raw);
    }

    @Override
    protected Batch createBatch(BatchContext raw) {
        return new AbstractExecutorProxy.Batch(raw) {};
    }
}
