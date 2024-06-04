package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import org.jooq.types.ULong;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class HyperLogLog extends AbstractYdbAggregateFunction<ULong> {

    private static final Name HYPER_LOG_LOG = systemName("HyperLogLog");

    public HyperLogLog(Field<?> field) {
        super(
                false,
                HYPER_LOG_LOG,
                YdbTypes.UINT64,
                field
        );
    }
}
