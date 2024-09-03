package tech.ydb.jooq.dsl.replace;

import java.util.Collection;
import java.util.Map;
import org.jooq.CheckReturnValue;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.Table;
import tech.ydb.jooq.Replace;
import tech.ydb.jooq.YdbDSLContext;

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
 * <h2>Referencing <code>XYZ*Step</code> types directly from client code</h2>
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
public interface ReplaceSetStep<R extends Record> {

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    ReplaceValuesStepN<R> columns(Field<?>... fields);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    ReplaceValuesStepN<R> columns(Collection<? extends Field<?>> fields);



    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1> ReplaceValuesStep1<R, T1> columns(Field<T1> field1);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2> ReplaceValuesStep2<R, T1, T2> columns(Field<T1> field1, Field<T2> field2);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3> ReplaceValuesStep3<R, T1, T2, T3> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4> ReplaceValuesStep4<R, T1, T2, T3, T4> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5> ReplaceValuesStep5<R, T1, T2, T3, T4, T5> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6> ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7> ReplaceValuesStep7<R, T1, T2, T3, T4, T5, T6, T7> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8> ReplaceValuesStep8<R, T1, T2, T3, T4, T5, T6, T7, T8> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9> ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> ReplaceValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> ReplaceValuesStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> ReplaceValuesStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> ReplaceValuesStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> ReplaceValuesStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> ReplaceValuesStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> ReplaceValuesStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> ReplaceValuesStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> ReplaceValuesStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> ReplaceValuesStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> ReplaceValuesStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> ReplaceValuesStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21);

    /**
     * Set the columns for replace.
     */
    @CheckReturnValue
    <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> ReplaceValuesStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> columns(Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21, Field<T22> field22);



    /**
     * Set a value for a field in the <code>REPLACE</code> statement.
     */
    @CheckReturnValue
    <T> ReplaceSetMoreStep<R> set(Field<T> field, T value);

    /**
     * Set a value for a field in the <code>REPLACE</code> statement.
     */
    @CheckReturnValue
    <T> ReplaceSetMoreStep<R> set(Field<T> field, Field<T> value);

    /**
     * Set a value for a field in the <code>REPLACE</code> statement.
     */
    @CheckReturnValue
    <T> ReplaceSetMoreStep<R> set(Field<T> field, Select<? extends Record1<T>> value);

    /**
     * Set a <code>null</code> value for a field in the <code>REPLACE</code>
     * statement.
     * <p>
     * This method is convenience for calling {@link #set(Field, Object)},
     * without the necessity of casting the Java <code>null</code> literal to
     * <code>(T)</code>.
     */
    @CheckReturnValue
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
    @CheckReturnValue
    ReplaceSetMoreStep<R> set(Map<?, ?> map);

    /**
     * Set values in the <code>REPLACE</code> statement.
     * <p>
     * This is the same as calling {@link #set(Map)} with the argument record
     * treated as a <code>Map&lt;Field&lt;?&gt;, Object&gt;</code>, except that the
     * {@link Record#changed()} flags are taken into consideration in order to
     * update only changed values.
     *
     * @see #set(Map)
     */
    @CheckReturnValue
    ReplaceSetMoreStep<R> set(Record record);

    /**
     * Set values in the <code>REPLACE</code> statement.
     * <p>
     * This is convenience for multiple calls to {@link #set(Record)} and
     * {@link ReplaceSetMoreStep#newRecord()}.
     *
     * @see #set(Record)
     */
    @CheckReturnValue
    ReplaceSetMoreStep<R> set(Record... records);

    /**
     * Set values in the <code>REPLACE</code> statement.
     * <p>
     * This is convenience for multiple calls to {@link #set(Record)} and
     * {@link ReplaceSetMoreStep#newRecord()}.
     *
     * @see #set(Record)
     */
    @CheckReturnValue
    ReplaceSetMoreStep<R> set(Collection<? extends Record> records);

    /**
     * Add values to the replace statement with implicit field names.
     */
    @CheckReturnValue
    ReplaceValuesStepN<R> values(Object... values);

    /**
     * Add values to the replace statement with implicit field names.
     */
    @CheckReturnValue
    ReplaceValuesStepN<R> values(Field<?>... values);

    /**
     * Add values to the replace statement with implicit field names.
     */
    @CheckReturnValue
    ReplaceValuesStepN<R> values(Collection<?> values);

    /**
     * Use a <code>SELECT</code> statement as the source of values for the
     * <code>REPLACE</code> statement.
     * <p>
     * This variant of the <code>REPLACE … SELECT</code> statement does not
     * allow for specifying a subset of the fields replaceed into. It will replace
     * into all fields of the table specified in the <code>INTO</code> clause.
     * Use {@link YdbDSLContext#replaceInto(Table, Field...)} or
     * {@link YdbDSLContext#replaceInto(Table, Collection)} instead, to
     * define a field set for replaceion.
     */
    @CheckReturnValue
    Replace<R> select(Select<?> select);
}
