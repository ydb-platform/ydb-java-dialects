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

    public <R> R transaction(RetryConfig config, Supplier<R> block) {
        if (config.maxAttempts() == 1) {
            return transaction(block);
        }

        if (getConnectionManager() instanceof YdbTxConnectionManager ydbCM) {
            return ydbCM.executeTransaction(config, con -> block.get());
        }

        throw new IllegalStateException(
                "The connection manager does not support retries for transactions. " +
                        "Use YdbTxConnectionManager."
        );
    }

    public <R> R withIsolation(
            RetryConfig config,
            int isolationLevel,
            boolean readOnly,
            Supplier<R> block
    ) {
        try {
            TransactionContext.setSettings(isolationLevel, readOnly);
            return transaction(config, block);
        } finally {
            TransactionContext.clear();
        }
    }

    public <R> R withIsolation(int isolationLevel, boolean readOnly, Supplier<R> block) {
        return withIsolation(RetryConfig.DEFAULT, isolationLevel, readOnly, block);
    }

    public <R> R serializableReadWrite(RetryConfig config, Supplier<R> block) {
        return withIsolation(config, Connection.TRANSACTION_SERIALIZABLE, false, block);
    }

    public <R> R serializableReadWrite(Supplier<R> block) {
        return serializableReadWrite(RetryConfig.DEFAULT, block);
    }

    public <R> R snapshotReadOnly(RetryConfig config, Supplier<R> block) {
        return withIsolation(config, Connection.TRANSACTION_SERIALIZABLE, true, block);
    }

    public <R> R snapshotReadOnly(Supplier<R> block) {
        return snapshotReadOnly(RetryConfig.DEFAULT, block);
    }

    public <R> R staleReadOnly(RetryConfig config, Supplier<R> block) {
        return withIsolation(config, YdbConst.STALE_READ_ONLY, true, block);
    }

    public <R> R staleReadOnly(Supplier<R> block) {
        return staleReadOnly(RetryConfig.DEFAULT, block);
    }

    public <R> R onlineConsistentReadOnly(RetryConfig config, Supplier<R> block) {
        return withIsolation(config, YdbConst.ONLINE_CONSISTENT_READ_ONLY, true, block);
    }

    public <R> R onlineConsistentReadOnly(Supplier<R> block) {
        return onlineConsistentReadOnly(RetryConfig.DEFAULT, block);
    }

    public <R> R onlineInconsistentReadOnly(RetryConfig config, Supplier<R> block) {
        return withIsolation(config, YdbConst.ONLINE_INCONSISTENT_READ_ONLY, true, block);
    }

    public <R> R onlineInconsistentReadOnly(Supplier<R> block) {
        return onlineInconsistentReadOnly(RetryConfig.DEFAULT, block);
    }
}
