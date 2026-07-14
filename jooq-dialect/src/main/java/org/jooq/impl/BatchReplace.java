package org.jooq.impl;

import org.jooq.BatchBindStep;
import org.jooq.Field;
import org.jooq.TableRecord;
import org.jooq.exception.DataAccessException;
import org.reactivestreams.Subscriber;
import tech.ydb.jooq.ReplaceQuery;
import tech.ydb.jooq.YdbDSLContext;

import java.util.function.Function;

public class BatchReplace extends AbstractBatch {
    private final YdbDSLContext ydbDSLContext;
    private final TableRecord<?>[] records;

    public BatchReplace(YdbDSLContext ydbDSLContext, TableRecord<?>[] records) {
        super(ydbDSLContext.configuration());
        this.ydbDSLContext = ydbDSLContext;
        this.records = records;
    }

    @Override
    public int[] execute() throws DataAccessException {
        return execute0(BatchBindStep::execute, new int[0]);
    }

    @Override
    public long[] executeLarge() throws DataAccessException {
        return execute0(BatchBindStep::executeLarge, new long[0]);
    }

    private <T> T execute0(Function<BatchBindStep, T> executor, T empty) {
        BatchBindStep batchBindStep = prepareBatch();
        if (batchBindStep == null) {
            return empty;
        }
        T result = executor.apply(batchBindStep);
        updateChangedFlag();
        return result;
    }

    @Override
    public int size() {
        return records.length;
    }

    @Override
    void subscribe0(Subscriber<? super R2DBC.RowCount> subscriber) {
        throw new UnsupportedOperationException("BatchReplace operation is not supported in a reactive way");
    }

    private BatchBindStep prepareBatch() {
        BatchBindStep batch = null;

        for (TableRecord<?> record : records) {
            ReplaceQuery<?> upsertQuery = ydbDSLContext.replaceQuery(record.getTable());

            for (Field<?> field : record.fields()) {
                upsertQuery.addValue((Field<Object>) field, record.get(field));
            }

            if (batch == null) {
                batch = ydbDSLContext.batch(upsertQuery);
            }

            batch.bind(upsertQuery.getBindValues().toArray());
        }

        return batch;
    }

    private void updateChangedFlag() {
        for (TableRecord<?> record : records) {
            if (record instanceof AbstractRecord r) {
                r.fetched = true;
            }
        }
    }
}
