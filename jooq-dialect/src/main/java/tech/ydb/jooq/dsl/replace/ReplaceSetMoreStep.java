package tech.ydb.jooq.dsl.replace;

import org.jetbrains.annotations.NotNull;
import org.jooq.CheckReturnValue;
import org.jooq.*;
import org.jooq.Record;
import tech.ydb.jooq.Replace;
import tech.ydb.jooq.ReplaceQuery;

import java.util.Collection;
import java.util.Map;

/**
 * This type is used for the {@link Replace}'s alternative DSL API.
 * <p>
 * Example: <pre><code>
 * YdbDSLContext create = YDB.using(configuration);
 *
 * create.replaceInto(table)
 *       .set(field1, value1)
 *       .set(field2, value2)
 *       .newRecord()
 *       .set(field1, value3)
 *       .set(field2, value4)
 *       .execute();
 * </code></pre>
 * <p>
 * <h3>Referencing <code>XYZ*Step</code> types directly from client code</h3>
 * <p>
 * It is usually not recommended to reference any <code>XYZ*Step</code> types
 * directly from client code, or assign them to local variables. When writing
 * dynamic SQL, creating a statement's components dynamically, and passing them
 * to the DSL API statically is usually a better choice. See the manual's
 * section about dynamic SQL for details: <a href=
 * "https://www.jooq.org/doc/latest/manual/sql-building/dynamic-sql">https://www.jooq.org/doc/latest/manual/sql-building/dynamic-sql</a>.
 * <p>
 * Drawbacks of referencing the <code>XYZ*Step</code> types directly:
 * <ul>
 * <li>They're operating on mutable implementations (as of jOOQ 3.x)</li>
 * <li>They're less composable and not easy to get right when dynamic SQL gets
 * complex</li>
 * <li>They're less readable</li>
 * <li>They might have binary incompatible changes between minor releases</li>
 * </ul>
 */
public interface ReplaceSetMoreStep<R extends Record> extends Replace<R> {

    /**
     * Set a value for a field in the <code>REPLACE</code> statement.
     */
    @NotNull @CheckReturnValue
    <T> ReplaceSetMoreStep<R> set(Field<T> field, T value);

    /**
     * Set a value for a field in the <code>REPLACE</code> statement.
     */
    @NotNull @CheckReturnValue
    <T> ReplaceSetMoreStep<R> set(Field<T> field, Field<T> value);

    /**
     * Set a value for a field in the <code>REPLACE</code> statement.
     */
    @NotNull @CheckReturnValue
    <T> ReplaceSetMoreStep<R> set(Field<T> field, Select<? extends Record1<T>> value);

    /**
     * Set a <code>null</code> value for a field in the <code>REPLACE</code>
     * statement.
     * <p>
     * This method is convenience for calling {@link #set(Field, Object)},
     * without the necessity of casting the Java <code>null</code> literal to
     * <code>(T)</code>.
     */
    @NotNull @CheckReturnValue
    <T> ReplaceSetMoreStep<R> setNull(Field<T> field);

    /**
     * Set values in the <code>REPLACE</code> statement.
     * <p>
     * Keys can either be of type {@link String}, {@link Name}, or
     * {@link Field}.
     * <p>
     * Values can either be of type <code>&lt;T&gt;</code> or
     * <code>Field&lt;T&gt;</code>. jOOQ will attempt to convert values to their
     * corresponding field's type.
     */
    @NotNull @CheckReturnValue
    ReplaceSetMoreStep<R> set(Map<?, ?> map);

    /**
     * Set values in the <code>REPLACE</code> statement.
     * <p>
     * This is the same as calling {@link #set(Map)} with the argument record
     * treated as a <code>Map&lt;Field&lt;?&gt;, Object&gt;</code>.
     *
     * @see #set(Map)
     */
    @NotNull @CheckReturnValue
    ReplaceSetMoreStep<R> set(Record record);

    /**
     * Set values in the <code>REPLACE</code> statement.
     * <p>
     * This is convenience for multiple calls to {@link #set(Record)} and
     * {@link ReplaceSetMoreStep#newRecord()}.
     *
     * @see #set(Record)
     */
    @NotNull @CheckReturnValue
    ReplaceSetMoreStep<R> set(Record... records);

    /**
     * Set values in the <code>REPLACE</code> statement.
     * <p>
     * This is convenience for multiple calls to {@link #set(Record)} and
     * {@link ReplaceSetMoreStep#newRecord()}.
     *
     * @see #set(Record)
     */
    @NotNull @CheckReturnValue
    ReplaceSetMoreStep<R> set(Collection<? extends Record> records);

    /**
     * Add an additional record to the <code>REPLACE</code> statement
     *
     * @see ReplaceQuery#newRecord()
     */
    @NotNull @CheckReturnValue
    ReplaceSetStep<R> newRecord();
}
