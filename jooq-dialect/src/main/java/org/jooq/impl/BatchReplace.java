package org.jooq.impl;

import org.jooq.BatchBindStep;
import org.jooq.Field;
import org.jooq.TableRecord;
import org.jooq.exception.DataAccessException;
import org.reactivestreams.Subscriber;
import tech.ydb.jooq.ReplaceQuery;
import tech.ydb.jooq.YdbDSLContext;

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
        BatchBindStep batchBindStep = prepareBatch();
        if (batchBindStep == null) {
            return new int[0];
        }
        int[] result = batchBindStep.execute();
        updateChangedFlag();
        return result;
    }

    @Override
    public int size() {
        return records.length;
    }

    @Override
    public void subscribe(Subscriber<? super Integer> subscriber) {
        throw new UnsupportedOperationException("BatchUpsert operation is not supported in a reactive way");
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
