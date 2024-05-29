package tech.ydb.jooq.dsl.upsert;

import org.jetbrains.annotations.NotNull;
import org.jooq.Record;
import org.jooq.*;
import tech.ydb.jooq.Upsert;
import tech.ydb.jooq.YdbDSLContext;

import java.util.Collection;
import java.util.function.Function;

/**
 * This type is used for the {@link Upsert}'s DSL API.
 * <p>
 * Example: <pre><code>
 * using(configuration)
 *       .upsertInto(table, field1, field2, field3, .., field9, field10)
 *       .values(valueA1, valueA2, valueA3, .., valueA9, valueA10)
 *       .values(valueB1, valueB2, valueB3, .., valueB9, valueB10)
 *       .execute();
 * </code></pre>
 */
public interface UpsertValuesStep10<R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> extends Upsert<R> {

    /**
     * Add a single row of values to the upsert statement.
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10);

    /**
     * Add a single row of values to the upsert statement.
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10);

    /**
     * Add a single row of values to the upsert statement.
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> values(Collection<?> values);

    /**
     * Add a single row of values to the upsert statement.
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> values(Row10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> values);

    /**
     * Add a single row of values to the upsert statement.
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> values(Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> values);

    /**
     * Add multiple rows of values to the upsert statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowArray(Function, Function, Function, Function, Function, Function, Function, Function, Function, Function)
     */
    @NotNull @CheckReturnValue
    @SuppressWarnings("unchecked")
    UpsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> valuesOfRows(Row10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>... values);

    /**
     * Add multiple rows of values to the upsert statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowList(Function, Function, Function, Function, Function, Function, Function, Function, Function, Function)
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> valuesOfRows(Collection<? extends Row10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> values);

    /**
     * Add multiple rows of values to the upsert statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     * <p>
     * <strong>Note</strong>: Irrespective of individual {@link Record#changed()}
     * flag values, all record values are copied to the <code>VALUES</code> clause
     * using {@link Record#intoArray()}, to match upsert columns by position, not
     * by name. If you prefer omitting unchanged values and adding values by field
     * name rather than by index, use {@link UpsertSetStep#set(Record...)} instead.
     * That syntax is available only if you omit the explicit upsert columns list.
     */
    @NotNull @CheckReturnValue
    @SuppressWarnings("unchecked")
    UpsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> valuesOfRecords(Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>... values);

    /**
     * Add multiple rows of values to the upsert statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     * <p>
     * <strong>Note</strong>: Irrespective of individual {@link Record#changed()}
     * flag values, all record values are copied to the <code>VALUES</code> clause
     * using {@link Record#intoArray()}, to match upsert columns by position, not
     * by name. If you prefer omitting unchanged values and adding values by field
     * name rather than by index, use {@link UpsertSetStep#set(Record...)} instead.
     * That syntax is available only if you omit the explicit upsert columns list.
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> valuesOfRecords(Collection<? extends Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> values);

    /**
     * Use a <code>SELECT</code> statement as the source of values for the
     * <code>UPSERT</code> statement
     * <p>
     * This variant of the <code>UPSERT … SELECT</code> statement expects a
     * select returning exactly as many fields as specified previously in the
     * <code>INTO</code> clause:
     * {@link YdbDSLContext#upsertInto(Table, Field, Field, Field, Field, Field, Field, Field, Field, Field, Field)}
     */
    @NotNull @CheckReturnValue
    Upsert<R> select(Select<? extends Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>> select);
}
