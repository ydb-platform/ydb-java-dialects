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
 *       .upsertInto(table, field1, field2)
 *       .values(field1, field2)
 *       .values(field1, field2)
 *       .execute();
 * </code></pre>
 */
public interface UpsertValuesStep2<R extends Record, T1, T2> extends Upsert<R> {

    /**
     * Add a single row of values to the upsert statement.
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep2<R, T1, T2> values(T1 value1, T2 value2);

    /**
     * Add a single row of values to the upsert statement.
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep2<R, T1, T2> values(Field<T1> value1, Field<T2> value2);

    /**
     * Add a single row of values to the upsert statement.
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep2<R, T1, T2> values(Collection<?> values);

    /**
     * Add a single row of values to the upsert statement.
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep2<R, T1, T2> values(Row2<T1, T2> values);

    /**
     * Add a single row of values to the upsert statement.
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep2<R, T1, T2> values(Record2<T1, T2> values);

    /**
     * Add multiple rows of values to the upsert statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowArray(Function, Function)
     */
    @NotNull @CheckReturnValue
    @SuppressWarnings("unchecked")
    UpsertValuesStep2<R, T1, T2> valuesOfRows(Row2<T1, T2>... values);

    /**
     * Add multiple rows of values to the upsert statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowList(Function, Function)
     */
    @NotNull @CheckReturnValue
    UpsertValuesStep2<R, T1, T2> valuesOfRows(Collection<? extends Row2<T1, T2>> values);

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
    UpsertValuesStep2<R, T1, T2> valuesOfRecords(Record2<T1, T2>... values);

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
    UpsertValuesStep2<R, T1, T2> valuesOfRecords(Collection<? extends Record2<T1, T2>> values);

    /**
     * Use a <code>SELECT</code> statement as the source of values for the
     * <code>UPSERT</code> statement
     * <p>
     * This variant of the <code>UPSERT … SELECT</code> statement expects a
     * select returning exactly as many fields as specified previously in the
     * <code>INTO</code> clause:
     * {@link YdbDSLContext#upsertInto(Table, Field, Field)}
     */
    @NotNull @CheckReturnValue
    Upsert<R> select(Select<? extends Record2<T1, T2>> select);
}
