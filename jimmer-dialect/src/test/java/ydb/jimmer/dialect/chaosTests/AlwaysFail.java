package ydb.jimmer.dialect.chaosTests;

import org.babyfish.jimmer.sql.exception.ExecutionException;

import java.sql.SQLTimeoutException;
import java.util.function.Supplier;

public class AlwaysFail<R> implements Supplier<R> {
    @Override
    public R get() {
        throw new ExecutionException("Persistent failure", new SQLTimeoutException());
    }
}
