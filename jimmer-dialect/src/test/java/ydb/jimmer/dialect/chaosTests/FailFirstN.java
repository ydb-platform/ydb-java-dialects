package ydb.jimmer.dialect.chaosTests;

import org.babyfish.jimmer.sql.exception.ExecutionException;
import ydb.jimmer.dialect.transaction.YdbVendorCode;

import java.sql.SQLException;
import java.util.function.Supplier;

public class FailFirstN<R> implements Supplier<R> {
    private final int n;
    private final R result;

    private int attempt;

    public FailFirstN(int n) {
        this.n = n;
        this.result = null;
    }

    public FailFirstN(int n, R result) {
        this.n = n;
        this.result = result;
    }

    public int getAttempt() {
        return attempt;
    }

    @Override
    public R get() {
        if (attempt++ < n) {
            throw new ExecutionException(
                    "Expected failure number " + attempt,
                    new SQLException("", "", YdbVendorCode.UNAVAILABLE)
            );
        }

        return result;
    }
}
