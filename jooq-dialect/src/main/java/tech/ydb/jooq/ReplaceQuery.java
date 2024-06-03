package tech.ydb.jooq;

import org.jooq.Record;
import org.jooq.*;

import java.util.Collection;

/**
 * An <code>REPLACE</code> statement (model API).
 * <p>
 * This type is the model API representation of a {@link Replace} statement,
 * which can be mutated after creation. The advantage of this API compared to
 * the DSL API is a more simple approach to writing dynamic SQL.
 * <p>
 * Instances can be created using {@link YdbDSLContext#replaceQuery(Table)} and
 * overloads.
 *
 * @param <R> The record type of the table being replaced into
 */
public interface ReplaceQuery<R extends Record> extends StoreQuery<R>, Replace<R> {

    /**
     * Adds a new Record to the replace statement for multi-record replacements
     * <p>
     * Calling this method will cause subsequent calls to
     * {@link #addValue(Field, Object)} (and similar) to fill the next record.
     * <p>
     * If this call is not followed by {@link #addValue(Field, Object)} calls,
     * then this call has no effect.
     * <p>
     * If this call is done on a fresh upsert statement (without any values
     * yet), then this call has no effect either.
     */
    void newRecord();

    /**
     * Short for calling {@link #newRecord()} and {@link #setRecord(Record)}.
     *
     * @param record The record to add to this upsert statement.
     */
    void addRecord(R record);

    /**
     * Use a <code>SELECT</code> statement as the source of values for the
     * <code>UPSERT</code> statement.
     */
    void setSelect(Field<?>[] fields, Select<?> select);

    /**
     * Use a <code>SELECT</code> statement as the source of values for the
     * <code>UPSERT</code> statement.
     */
    void setSelect(Collection<? extends Field<?>> fields, Select<?> select);
}