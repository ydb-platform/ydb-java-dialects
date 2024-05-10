package tech.ydb.jooq;

import org.jooq.DMLQuery;
import org.jooq.Record;
import org.jooq.Table;

/**
 * An <code>REPLACE</code> statement.
 * <p>
 * <strong>Example:</strong>
 * <pre><code>
 * // Assuming import static org.jooq.impl.DSL.* and tech.ydb.jooq.*;
 *
 * using(configuration)
 *    .replaceInto(ACTOR)
 *    .columns(ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
 *    .values("John", "Doe")
 *    .execute();
 * </code></pre>
 * <p>
 * Instances can be created using {@link YDB#replaceInto(Table)}, or
 * {@link YdbDSLContext#replaceQuery(Table)} and overloads.
 *
 * @param <R> the record type that is being manipulated by the REPLACE statement
 */
public interface Replace<R extends Record> extends DMLQuery<R> {

}
