package ydb.jimmer.dialect.transaction;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.di.AbstractJSqlClientDelegate;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import ydb.jimmer.dialect.constant.YdbConst;

import java.sql.Connection;
import java.util.function.Supplier;

/**
 * Provides methods for setting transaction isolation level
 * to the {@link JSqlClient}.
 */
public class YqlClient extends AbstractJSqlClientDelegate {
    private final JSqlClientImplementor delegate;

    public YqlClient(JSqlClientImplementor delegate) {
        this.delegate = delegate;
    }

    @Override
    protected JSqlClientImplementor sqlClient() {
        return delegate;
    }

    public <R> R transaction(int maxAttempts, long retryDelayMs, Supplier<R> block) {
        if (maxAttempts == 1) {
            return transaction(block);
        } else if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be a positive integer");
        } else if (retryDelayMs < 0) {
            throw new IllegalArgumentException("retryDelayMs must not be negative");
        }

        if (getConnectionManager() instanceof YdbTxConnectionManager ydbCM) {
            return ydbCM.executeTransaction(maxAttempts, retryDelayMs, con -> block.get());
        }

        throw new IllegalStateException(
                "The connection manager does not support retries for transactions. " +
                        "Use YdbTxConnectionManager."
        );
    }

    public <R> R withIsolation(
            int maxAttempts,
            long retryDelayMs,
            int isolationLevel,
            boolean readOnly,
            Supplier<R> block
    ) {
        try {
            TransactionContext.setSettings(isolationLevel, readOnly);
            return transaction(maxAttempts, retryDelayMs, block);
        } finally {
            TransactionContext.clear();
        }
    }

    public <R> R withIsolation(int isolationLevel, boolean readOnly, Supplier<R> block) {
        return withIsolation(1, 0, isolationLevel, readOnly, block);
    }

    public <R> R serializableReadWrite(int maxAttempts, long retryDelayMs, Supplier<R> block) {
        return withIsolation(maxAttempts, retryDelayMs, Connection.TRANSACTION_SERIALIZABLE, false, block);
    }

    public <R> R serializableReadWrite(Supplier<R> block) {
        return serializableReadWrite(1, 0, block);
    }

    public <R> R snapshotReadOnly(int maxAttempts, long retryDelayMs, Supplier<R> block) {
        return withIsolation(maxAttempts, retryDelayMs, Connection.TRANSACTION_SERIALIZABLE, true, block);
    }

    public <R> R snapshotReadOnly(Supplier<R> block) {
        return snapshotReadOnly(1, 0, block);
    }

    public <R> R staleReadOnly(int maxAttempts, long retryDelayMs, Supplier<R> block) {
        return withIsolation(maxAttempts, retryDelayMs, YdbConst.STALE_READ_ONLY, true, block);
    }

    public <R> R staleReadOnly(Supplier<R> block) {
        return staleReadOnly(1, 0, block);
    }

    public <R> R onlineConsistentReadOnly(int maxAttempts, long retryDelayMs, Supplier<R> block) {
        return withIsolation(YdbConst.ONLINE_CONSISTENT_READ_ONLY, true, block);
    }

    public <R> R onlineConsistentReadOnly(Supplier<R> block) {
        return onlineConsistentReadOnly(1, 0, block);
    }

    public <R> R onlineInconsistentReadOnly(int maxAttempts, long retryDelayMs, Supplier<R> block) {
        return withIsolation(YdbConst.ONLINE_INCONSISTENT_READ_ONLY, true, block);
    }

    public <R> R onlineInconsistentReadOnly(Supplier<R> block) {
        return onlineInconsistentReadOnly(1, 0, block);
    }
}
