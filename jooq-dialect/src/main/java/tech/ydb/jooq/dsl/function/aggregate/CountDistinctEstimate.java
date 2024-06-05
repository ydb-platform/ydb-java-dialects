package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import org.jooq.types.ULong;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class CountDistinctEstimate extends AbstractYdbAggregateFunction<ULong> {

    private static final Name COUNT_DISTINCT_ESTIMATE = systemName("CountDistinctEstimate");

    public CountDistinctEstimate(Field<?> field) {
        super(
                false,
                COUNT_DISTINCT_ESTIMATE,
                YdbTypes.UINT64,
                field
        );
    }
}
