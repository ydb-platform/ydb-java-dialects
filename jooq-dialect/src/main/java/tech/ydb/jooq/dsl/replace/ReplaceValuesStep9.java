package tech.ydb.jooq.dsl.replace;

import java.util.Collection;
import java.util.function.Function;
import org.jooq.CheckReturnValue;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.Rows;
import org.jooq.Select;
import org.jooq.Table;
import tech.ydb.jooq.Replace;
import tech.ydb.jooq.YdbDSLContext;

/**
 * This type is used for the {@link Replace}'s DSL API.
 * <p>
 * Example: <pre><code>
 * using(configuration)
 *       .replaceInto(table, field1, field2, field3, .., field8, field9)
 *       .values(valueA1, valueA2, valueA3, .., valueA8, valueA9)
 *       .values(valueB1, valueB2, valueB3, .., valueB8, valueB9)
 *       .execute();
 * </code></pre>
 */
public interface ReplaceValuesStep9<R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9> extends Replace<R> {

    /**
     * Add a single row of values to the replace statement.
     */
    @CheckReturnValue
    ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9);

    /**
     * Add a single row of values to the replace statement.
     */
    @CheckReturnValue
    ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9);

    /**
     * Add a single row of values to the replace statement.
     */
    @CheckReturnValue
    ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> values(Collection<?> values);

    /**
     * Add a single row of values to the replace statement.
     */
    @CheckReturnValue
    ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> values(Row9<T1, T2, T3, T4, T5, T6, T7, T8, T9> values);

    /**
     * Add a single row of values to the replace statement.
     */
    @CheckReturnValue
    ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> values(Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9> values);

    /**
     * Add multiple rows of values to the replace statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowArray(Function, Function, Function, Function, Function, Function, Function, Function, Function)
     */
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> valuesOfRows(Row9<T1, T2, T3, T4, T5, T6, T7, T8, T9>... values);

    /**
     * Add multiple rows of values to the replace statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowList(Function, Function, Function, Function, Function, Function, Function, Function, Function)
     */
    @CheckReturnValue
    ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> valuesOfRows(Collection<? extends Row9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> values);

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
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> valuesOfRecords(Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9>... values);

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
    @CheckReturnValue
    ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> valuesOfRecords(Collection<? extends Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> values);

    /**
     * Use a <code>SELECT</code> statement as the source of values for the
     * <code>REPLACE</code> statement
     * <p>
     * This variant of the <code>REPLACE … SELECT</code> statement expects a
     * select returning exactly as many fields as specified previously in the
     * <code>INTO</code> clause:
     * {@link YdbDSLContext#replaceInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field)}
     */
    @CheckReturnValue
    Replace<R> select(Select<? extends Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> select);
}
