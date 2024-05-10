package tech.ydb.jooq.dsl.replace;

import org.jetbrains.annotations.NotNull;
import org.jooq.Record;
import org.jooq.*;
import tech.ydb.jooq.Replace;
import tech.ydb.jooq.YdbDSLContext;

import java.util.Collection;
import java.util.function.Function;

/**
 * This type is used for the {@link Replace}'s DSL API.
 * <p>
 * Example: <pre><code>
 * using(configuration)
 *       .replaceInto(table, field1, field2, field3, .., field5, field6)
 *       .values(valueA1, valueA2, valueA3, .., valueA5, valueA6)
 *       .values(valueB1, valueB2, valueB3, .., valueB5, valueB6)
 *       .execute();
 * </code></pre>
 */
public interface ReplaceValuesStep6<R extends Record, T1, T2, T3, T4, T5, T6> extends Replace<R> {

    /**
     * Add a single row of values to the replace statement.
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6> values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6);

    /**
     * Add a single row of values to the replace statement.
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6> values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6);

    /**
     * Add a single row of values to the replace statement.
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6> values(Collection<?> values);

    /**
     * Add a single row of values to the replace statement.
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6> values(Row6<T1, T2, T3, T4, T5, T6> values);

    /**
     * Add a single row of values to the replace statement.
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6> values(Record6<T1, T2, T3, T4, T5, T6> values);

    /**
     * Add multiple rows of values to the replace statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowArray(Function, Function, Function, Function, Function, Function)
     */
    @NotNull @CheckReturnValue
    @SuppressWarnings("unchecked")
    ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6> valuesOfRows(Row6<T1, T2, T3, T4, T5, T6>... values);

    /**
     * Add multiple rows of values to the replace statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowList(Function, Function, Function, Function, Function, Function)
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6> valuesOfRows(Collection<? extends Row6<T1, T2, T3, T4, T5, T6>> values);

    /**
     * Add multiple rows of values to the replace statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     * <p>
     * <strong>Note</strong>: Irrespective of individual {@link Record#changed()}
     * flag values, all record values are copied to the <code>VALUES</code> clause
     * using {@link Record#intoArray()}, to match replace columns by position, not
     * by name. If you prefer omitting unchanged values and adding values by field
     * name rather than by index, use {@link ReplaceSetStep#set(Record...)} instead.
     * That syntax is available only if you omit the explicit replace columns list.
     */
    @NotNull @CheckReturnValue
    @SuppressWarnings("unchecked")
    ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6> valuesOfRecords(Record6<T1, T2, T3, T4, T5, T6>... values);

    /**
     * Add multiple rows of values to the replace statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     * <p>
     * <strong>Note</strong>: Irrespective of individual {@link Record#changed()}
     * flag values, all record values are copied to the <code>VALUES</code> clause
     * using {@link Record#intoArray()}, to match replace columns by position, not
     * by name. If you prefer omitting unchanged values and adding values by field
     * name rather than by index, use {@link ReplaceSetStep#set(Record...)} instead.
     * That syntax is available only if you omit the explicit replace columns list.
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6> valuesOfRecords(Collection<? extends Record6<T1, T2, T3, T4, T5, T6>> values);

    /**
     * Use a <code>SELECT</code> statement as the source of values for the
     * <code>REPLACE</code> statement
     * <p>
     * This variant of the <code>REPLACE … SELECT</code> statement expects a
     * select returning exactly as many fields as specified previously in the
     * <code>INTO</code> clause:
     * {@link YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field)}
     */
    @NotNull @CheckReturnValue
    Replace<R> select(Select<? extends Record6<T1, T2, T3, T4, T5, T6>> select);
}
