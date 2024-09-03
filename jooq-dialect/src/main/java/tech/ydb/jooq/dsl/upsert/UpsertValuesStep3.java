package tech.ydb.jooq.dsl.upsert;

import java.util.Collection;
import java.util.function.Function;
import org.jooq.CheckReturnValue;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.Rows;
import org.jooq.Select;
import org.jooq.Table;
import tech.ydb.jooq.Upsert;
import tech.ydb.jooq.YdbDSLContext;

/**
 * This type is used for the {@link Upsert}'s DSL API.
 * <p>
 * Example: <pre><code>
 * using(configuration)
 *       .upsertInto(table, field1, field2, field3)
 *       .values(field1, field2, field3)
 *       .values(field1, field2, field3)
 *       .execute();
 * </code></pre>
 */
public interface UpsertValuesStep3<R extends Record, T1, T2, T3> extends Upsert<R> {

    /**
     * Add a single row of values to the upsert statement.
     */
    @CheckReturnValue
    UpsertValuesStep3<R, T1, T2, T3> values(T1 value1, T2 value2, T3 value3);

    /**
     * Add a single row of values to the upsert statement.
     */
    @CheckReturnValue
    UpsertValuesStep3<R, T1, T2, T3> values(Field<T1> value1, Field<T2> value2, Field<T3> value3);

    /**
     * Add a single row of values to the upsert statement.
     */
    @CheckReturnValue
    UpsertValuesStep3<R, T1, T2, T3> values(Collection<?> values);

    /**
     * Add a single row of values to the upsert statement.
     */
    @CheckReturnValue
    UpsertValuesStep3<R, T1, T2, T3> values(Row3<T1, T2, T3> values);

    /**
     * Add a single row of values to the upsert statement.
     */
    @CheckReturnValue
    UpsertValuesStep3<R, T1, T2, T3> values(Record3<T1, T2, T3> values);

    /**
     * Add multiple rows of values to the upsert statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowArray(Function, Function, Function)
     */
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    UpsertValuesStep3<R, T1, T2, T3> valuesOfRows(Row3<T1, T2, T3>... values);

    /**
     * Add multiple rows of values to the upsert statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowList(Function, Function, Function)
     */
    @CheckReturnValue
    UpsertValuesStep3<R, T1, T2, T3> valuesOfRows(Collection<? extends Row3<T1, T2, T3>> values);

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
    @CheckReturnValue
    @SuppressWarnings("unchecked")
    UpsertValuesStep3<R, T1, T2, T3> valuesOfRecords(Record3<T1, T2, T3>... values);

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
    @CheckReturnValue
    UpsertValuesStep3<R, T1, T2, T3> valuesOfRecords(Collection<? extends Record3<T1, T2, T3>> values);

    /**
     * Use a <code>SELECT</code> statement as the source of values for the
     * <code>UPSERT</code> statement
     * <p>
     * This variant of the <code>UPSERT … SELECT</code> statement expects a
     * select returning exactly as many fields as specified previously in the
     * <code>INTO</code> clause:
     * {@link YdbDSLContext#upsertInto(Table, Field, Field, Field)}
     */
    @CheckReturnValue
    Upsert<R> select(Select<? extends Record3<T1, T2, T3>> select);
}
