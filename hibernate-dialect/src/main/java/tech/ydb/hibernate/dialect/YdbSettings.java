package tech.ydb.hibernate.dialect;

/**
 * Dialect-specific configuration settings for YDB.
 *
 * @author Kirill Kurdyukov
 */
public interface YdbSettings {

    /**
     * When {@code true}, FOR UPDATE / FOR SHARE lock hints are omitted from generated SQL.
     * YDB does not support {@code SELECT ... FOR UPDATE} or {@code FOR SHARE}.
     *
     * @see jakarta.persistence.LockModeType#PESSIMISTIC_WRITE
     * @see jakarta.persistence.LockModeType#PESSIMISTIC_READ
     */
    String IGNORE_LOCK_HINTS = "hibernate.dialect.ydb.ignore_lock_hints";
}
