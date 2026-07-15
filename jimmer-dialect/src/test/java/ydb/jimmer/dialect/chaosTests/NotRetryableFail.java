package ydb.jimmer.dialect.chaosTests;

import org.babyfish.jimmer.sql.exception.ExecutionException;

import java.sql.SQLException;
import java.util.function.Supplier;

public class NotRetryableFail<R> implements Supplier<R> {
    private int attempt;

    public int getAttempt() {
        return attempt;
    }

    @Override
    public R get() {
        attempt++;
        throw new ExecutionException("Not retryable failure", new SQLException());
    }
}
