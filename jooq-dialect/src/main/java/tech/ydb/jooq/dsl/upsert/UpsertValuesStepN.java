package tech.ydb.jooq.dsl.upsert;

import java.util.Collection;
import java.util.function.Function;
import org.jooq.CheckReturnValue;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RowN;
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
 *       .upsertInto(table, fields)
 *       .values(fields)
 *       .values(fields)
 *       .execute();
 * </code></pre>
 */
public interface UpsertValuesStepN<R extends Record> extends Upsert<R> {

    /**
     * Add a single row of values to the upsert statement.
     */
    @CheckReturnValue
    UpsertValuesStepN<R> values(Object... values);

    /**
     * Add a single row of values to the upsert statement.
     */
    @CheckReturnValue
    UpsertValuesStepN<R> values(Field<?>... values);

    /**
     * Add a single row of values to the upsert statement.
     */
    @CheckReturnValue
    UpsertValuesStepN<R> values(Collection<?> values);

    /**
     * Add a single row of values to the upsert statement.
     */
    @CheckReturnValue
    UpsertValuesStepN<R> values(RowN values);

    /**
     * Add a single row of values to the upsert statement.
     */
    @CheckReturnValue
    UpsertValuesStepN<R> values(Record values);

    /**
     * Add multiple rows of values to the upsert statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowArray(Function, Function)
     */
    @CheckReturnValue
    UpsertValuesStepN<R> valuesOfRows(RowN... values);

    /**
     * Add multiple rows of values to the upsert statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowList(Function, Function)
     */
    @CheckReturnValue
    UpsertValuesStepN<R> valuesOfRows(Collection<? extends RowN> values);

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
    UpsertValuesStepN<R> valuesOfRecords(Record... values);

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
    UpsertValuesStepN<R> valuesOfRecords(Collection<? extends Record> values);

    /**
     * Use a <code>SELECT</code> statement as the source of values for the
     * <code>UPSERT</code> statement
     * <p>
     * This variant of the <code>UPSERT … SELECT</code> statement expects a
     * select returning exactly as many fields as specified previously in the
     * <code>INTO</code> clause:
     * {@link YdbDSLContext#upsertInto(Table)}
     */
    @CheckReturnValue
    Upsert<R> select(Select<? extends Record> select);
}
