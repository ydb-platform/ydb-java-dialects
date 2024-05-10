package tech.ydb.jooq;

import org.jooq.DMLQuery;
import org.jooq.Record;
import org.jooq.Table;

/**
 * An <code>UPSERT</code> statement.
 * <p>
 * <strong>Example:</strong>
 * <p>
 * <pre><code>
 * // Assuming import static org.jooq.impl.DSL.* and tech.ydb.jooq.*;
 *
 * using(configuration)
 *    .upsertInto(ACTOR)
 *    .columns(ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
 *    .values("John", "Doe")
 *    .execute();
 * </code></pre>
 * <p>
 * Instances can be created using {@link YDB#upsertInto(Table)}, or
 * {@link YdbDSLContext#upsertQuery(Table)} and overloads.
 */
public interface Upsert<R extends Record> extends DMLQuery<R> {

}
