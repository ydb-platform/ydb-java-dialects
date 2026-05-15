package ydb.jimmer.dialect.transaction;

import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.transaction.AbstractTxConnectionManager;
import org.babyfish.jimmer.sql.transaction.Propagation;
import org.babyfish.jimmer.sql.transaction.TxConnectionManager;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.function.Function;

/**
 * Provides propagation, isolation level, read only mode
 * and retry from abort/timeout for transactions.
 * This class is a modified version of {@link AbstractTxConnectionManager}.
 */
public class YdbTxConnectionManager implements TxConnectionManager {
    private final DataSource dataSource;
    private final ThreadLocal<Scope> scopeLocal = new ThreadLocal<>();

    private final int maxAttempts;
    private final long retryDelayMs;

    public YdbTxConnectionManager(DataSource dataSource) {
        this.dataSource = dataSource;
        this.maxAttempts = 1;
        this.retryDelayMs = 0;
    }

    public YdbTxConnectionManager(DataSource dataSource, int maxAttempts, long retryDelayMs) {
        this.dataSource = dataSource;
        this.maxAttempts = maxAttempts;
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public final <R> R execute(@Nullable Connection con, Function<Connection, R> block) {
        if (con != null) {
            return block.apply(con);
        }
        return execute(block);
    }

    @Override
    public final <R> R execute(Function<Connection, R> block) {
        return executeTransaction(Propagation.SUPPORTS, block);
    }

    @Override
    public final <R> R executeTransaction(Function<Connection, R> block) {
        Propagation propagation = Propagation.REQUIRED;
        if (maxAttempts > 1) {
            propagation = Propagation.REQUIRES_NEW;
        }
        return executeTransaction(propagation, block);
    }

    @Override
    public final <R> R executeTransaction(Propagation propagation, Function<Connection, R> block) {
        int maxAttempts = this.maxAttempts;
        if (maxAttempts > 1 && propagation != Propagation.REQUIRES_NEW) {
            maxAttempts = 1;
        }

        for (int i = 0; i < maxAttempts; i++) {
            try {
                Scope parent = scopeLocal.get();
                Scope scope = createScope(parent, propagation);
                scopeLocal.set(scope);
                try {
                    R result;
                    try {
                        result = execute(scope.con, block);
                    } catch (RuntimeException | Error ex) {
                        scope.terminate(true);
                        throw ex;
                    }
                    scope.terminate(false);
                    return result;
                } finally {
                    if (parent != null) {
                        scopeLocal.set(parent);
                    } else {
                        scopeLocal.remove();
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                if (i == maxAttempts - 1 || !isRetryable(ex)) {
                    throw (ex instanceof RuntimeException re) ? re : new RuntimeException(ex);
                }
            }

            try {
                Thread.sleep(retryDelayMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
        }

        throw new RuntimeException("Max attempts exceeded");
    }

    private boolean isRetryable(Exception ex) {
        return ex instanceof SQLTimeoutException ||
                (ex instanceof SQLException && isRetryableSqlState((SQLException) ex));
    }

    private boolean isRetryableSqlState(SQLException ex) {
        String sqlState = ex.getSQLState();
        return sqlState != null && (
                sqlState.startsWith("40") || // Transaction rollback
                        "08S01".equals(sqlState)   // Communication link failure
        );
    }

    protected Connection openConnection() throws SQLException {
        return dataSource.getConnection();
    }

    protected void closeConnection(Connection con) throws SQLException {
        con.close();
    }

    protected void startTransaction(Connection con) throws SQLException {
        con.setAutoCommit(false);

        TransactionContext.TransactionSettings settings = TransactionContext.getSettings();
        if (settings != null) {
            if (settings.isolationLevel != Connection.TRANSACTION_NONE) {
                con.setTransactionIsolation(settings.isolationLevel);
            }
            con.setReadOnly(settings.readOnly);
        }
    }

    protected void commitTransaction(Connection con) throws SQLException {
        con.commit();
    }

    protected void rollbackTransaction(Connection con) throws SQLException {
        con.rollback();
    }

    protected void abortTransaction(Connection con) throws SQLException {
        con.setAutoCommit(true);
    }

    private Scope createScope(Scope parent, Propagation propagation) throws SQLException {
        return switch (propagation) {
            case REQUIRES_NEW -> new Scope(parent, false, true);
            case SUPPORTS -> new Scope(parent, true, parent != null && parent.withTransaction);
            case NOT_SUPPORTED -> new Scope(parent, true, false);
            case MANDATORY -> {
                if (parent == null || !parent.withTransaction) {
                    throw new ExecutionException(
                            "The transaction propagation is \"MANDATORY\" but there is no transaction context"
                    );
                }
                yield new Scope(parent, true, true);
            }
            case NEVER -> {
                if (parent != null && parent.withTransaction) {
                    throw new ExecutionException(
                            "The transaction propagation is \"NEVER\" but there is already a transaction context"
                    );
                }
                yield new Scope(parent, true, false);
            }
            default ->  // REQUIRED
                    new Scope(parent, true, true);
        };
    }

    private class Scope {
        private final Connection con;

        private final boolean withTransaction;
        private final boolean connectionOwner;
        private final boolean transactionOwner;

        Scope(Scope parent, boolean borrowConnection, boolean withTransaction) throws SQLException {
            if (parent != null && parent.withTransaction && !withTransaction) {
                borrowConnection = false;
            }

            Connection con;
            if (parent != null && borrowConnection) {
                con = parent.con;
                this.connectionOwner = false;
            } else {
                con = openConnection();
                this.connectionOwner = true;
            }

            this.withTransaction = withTransaction;
            if (!withTransaction) {
                transactionOwner = false;
            } else if (connectionOwner) {
                transactionOwner = true;
            } else {
                transactionOwner = !parent.withTransaction;
            }

            if (transactionOwner) {
                try {
                    startTransaction(con);
                } catch (SQLException | RuntimeException | Error ex) {
                    closeConnection(con);
                    this.con = null;
                    throw ex;
                }
            }
            this.con = con;
        }

        void terminate(boolean error) throws SQLException {
            Connection con = this.con;
            if (con == null) {
                return;
            }

            try {
                if (transactionOwner) {
                    if (error) {
                        rollbackTransaction(con);
                    } else {
                        commitTransaction(con);
                    }
                    if (!connectionOwner) {
                        abortTransaction(con);
                    }
                }
            } finally {
                if (connectionOwner) {
                    closeConnection(con);
                }
            }
        }
    }
}
