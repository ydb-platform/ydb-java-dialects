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
 *       .replaceInto(table, fields)
 *       .values(fields)
 *       .values(fields)
 *       .execute();
 * </code></pre>
 */
public interface ReplaceValuesStepN<R extends Record> extends Replace<R> {

    /**
     * Add a single row of values to the replace statement.
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStepN<R> values(Object... values);

    /**
     * Add a single row of values to the replace statement.
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStepN<R> values(Field<?>... values);

    /**
     * Add a single row of values to the replace statement.
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStepN<R> values(Collection<?> values);

    /**
     * Add a single row of values to the replace statement.
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStepN<R> values(RowN values);

    /**
     * Add a single row of values to the replace statement.
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStepN<R> values(Record values);

    /**
     * Add multiple rows of values to the replace statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowArray(Function, Function)
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStepN<R> valuesOfRows(RowN... values);

    /**
     * Add multiple rows of values to the replace statement.
     * <p>
     * This is equivalent to calling the other values clauses multiple times, but
     * allows for dynamic construction of row arrays.
     *
     * @see Rows#toRowList(Function, Function)
     */
    @NotNull @CheckReturnValue
    ReplaceValuesStepN<R> valuesOfRows(Collection<? extends RowN> values);

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
    ReplaceValuesStepN<R> valuesOfRecords(Record... values);

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
    ReplaceValuesStepN<R> valuesOfRecords(Collection<? extends Record> values);

    /**
     * Use a <code>SELECT</code> statement as the source of values for the
     * <code>REPLACE</code> statement
     * <p>
     * This variant of the <code>REPLACE … SELECT</code> statement expects a
     * select returning exactly as many fields as specified previously in the
     * <code>INTO</code> clause:
     * {@link YdbDSLContext#replaceInto(Table)}
     */
    @NotNull @CheckReturnValue
    Replace<R> select(Select<? extends Record> select);
}
