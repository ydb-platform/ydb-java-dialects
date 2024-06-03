package tech.ydb.jooq;

import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.util.Properties;

/**
 * A resourceful {@link YdbDSLContext} that should be closed in a
 * try-with-resources statement.
 */
public interface CloseableYdbDSLContext extends YdbDSLContext, AutoCloseable {
    /**
     * Close the underlying resources, if any resources have been allocated when
     * constructing this <code>YdbDslContext</code>.
     * <p>
     * Some {@link YdbDSLContext} constructors, such as {@link DSL#using(String)},
     * {@link DSL#using(String, Properties)}, or
     * {@link DSL#using(String, String, String)} allocate a {@link Connection}
     * resource, which is inaccessible to the outside of the {@link YdbDSLContext}
     * implementation. Proper resource management must thus be done via this
     * {@link #close()} method.
     *
     * @throws DataAccessException When something went wrong closing the
     *             underlying resources.
     */
    @Override
    void close() throws DataAccessException;
}
