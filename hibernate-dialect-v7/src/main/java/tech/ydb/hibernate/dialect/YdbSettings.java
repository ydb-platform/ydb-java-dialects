package tech.ydb.hibernate.dialect;

/**
 * Dialect-specific configuration settings for YDB.
 *
 * @author Ainur Mukhtarov
 */
public interface YdbSettings {

    /**
     * Specifies whether the dialect should ignore FOR UPDATE / FOR SHARE lock hints
     * and omit them from generated SQL.
     * YDB does not support {@code SELECT ... FOR UPDATE} or {@code FOR SHARE}.
     * When this setting is {@code true}, the dialect returns an empty string for
     * lock clauses, allowing applications that use
     * {@code PESSIMISTIC_WRITE} to run without errors.
     * Set to {@code false} to throw {@link UnsupportedOperationException} when
     * lock hints are requested, making the lack of support explicit.
     *
     * By default, it is disabled to support backward compatibility
     *
     * @see jakarta.persistence.LockModeType#PESSIMISTIC_WRITE
     * @see jakarta.persistence.LockModeType#PESSIMISTIC_READ
     */
    String IGNORE_LOCK_HINTS = "hibernate.dialect.ydb.ignore_lock_hints";
}
