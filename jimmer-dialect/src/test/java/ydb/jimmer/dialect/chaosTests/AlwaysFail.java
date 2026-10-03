package ydb.jimmer.dialect.chaosTests;

import org.babyfish.jimmer.sql.exception.ExecutionException;
import ydb.jimmer.dialect.transaction.YdbVendorCode;

import java.sql.SQLException;
import java.util.function.Supplier;

public class AlwaysFail<R> implements Supplier<R> {
    @Override
    public R get() {
        throw new ExecutionException(
                "Persistent failure",
                new SQLException("", "", YdbVendorCode.UNAVAILABLE)
        );
    }
}
