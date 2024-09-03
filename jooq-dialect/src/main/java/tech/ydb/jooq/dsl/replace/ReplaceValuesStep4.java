package tech.ydb.jooq.dsl.replace;

import java.util.Collection;
import java.util.function.Function;
import org.jooq.CheckReturnValue;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.Row4;
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
 *       .replaceInto(table, field1, field2, field3, field4)
 *       .values(field1, field2, field3, field4)
 *       .values(field1, field2, field3, field4)
 *       .execute();
 * </code></pre>
 */
public interface ReplaceValuesStep4<R extends Record, T1, T2, T3, T4> extends Replace<R> {

    /**
     * Add a single row of values to the replace statement.
     */
    @CheckReturnValue
    ReplaceValuesStep4<R, T1, T2, T3, T4> values(T1 value1, T2 value2, T3 value3, T4 value4);

    /**
     * Add a single row of values to the replace statement.
     */
    @CheckReturnValue
    ReplaceValuesStep4<R, T1, T2, T3, T4> values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4);

    /**
     * Add a single row of values to the replace statement.
     */
    @CheckReturnValue
    ReplaceValuesStep4<R, T1, T2, T3, T4> values(Collection<?> values);

    /**
     * Add a single row of values to the replace statement.
     */
    @CheckReturnValue
    ReplaceValuesStep4<R, T1, T2, T3, T4> values(Row4<T1, T2, T3, T4> values);

    /**
     * Add a single row of values to the replace statement.
     */
    @CheckReturnValue
    ReplaceValuesStep4<R, T1, T2, T3, T4> values(Record4<T1, T2, T3, T4> values);

    /**
     * Add multiple rows of values to the replace statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowArray(Function, Function, Function, Function)
     */
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    ReplaceValuesStep4<R, T1, T2, T3, T4> valuesOfRows(Row4<T1, T2, T3, T4>... values);

    /**
     * Add multiple rows of values to the replace statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowList(Function, Function, Function, Function)
     */
    @CheckReturnValue
    ReplaceValuesStep4<R, T1, T2, T3, T4> valuesOfRows(Collection<? extends Row4<T1, T2, T3, T4>> values);

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
    ReplaceValuesStep4<R, T1, T2, T3, T4> valuesOfRecords(Record4<T1, T2, T3, T4>... values);

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
    ReplaceValuesStep4<R, T1, T2, T3, T4> valuesOfRecords(Collection<? extends Record4<T1, T2, T3, T4>> values);

    /**
     * Use a <code>SELECT</code> statement as the source of values for the
     * <code>REPLACE</code> statement
     * <p>
     * This variant of the <code>REPLACE … SELECT</code> statement expects a
     * select returning exactly as many fields as specified previously in the
     * <code>INTO</code> clause:
     * {@link YdbDSLContext#replaceInto(Table, Field, Field, Field, Field)}
     */
    @CheckReturnValue
    Replace<R> select(Select<? extends Record4<T1, T2, T3, T4>> select);
}
