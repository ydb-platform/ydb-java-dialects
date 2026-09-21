package ydb.jimmer.dialect.transaction;

/**
 * This class is used to pass transaction setting
 * to the ConnectionManager.
 */
public class TransactionContext {
    private static final ThreadLocal<TransactionSettings> LOCAL_CONTEXT = new ThreadLocal<>();

    public static void setSettings(int isolationLevel, boolean readOnly) {
        LOCAL_CONTEXT.set(new TransactionSettings(isolationLevel, readOnly));
    }

    public static TransactionSettings getSettings() {
        return LOCAL_CONTEXT.get();
    }

    public static void clear() {
        LOCAL_CONTEXT.remove();
    }

    public static class TransactionSettings {
        final int isolationLevel;
        final boolean readOnly;

        TransactionSettings(int isolationLevel, boolean readOnly) {
            this.isolationLevel = isolationLevel;
            this.readOnly = readOnly;
        }
    }
}
