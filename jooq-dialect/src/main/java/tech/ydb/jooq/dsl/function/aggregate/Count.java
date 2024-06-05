package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import org.jooq.types.ULong;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class Count extends AbstractYdbAggregateFunction<ULong> {

    private static final Name COUNT = systemName("Count");

    public Count(Field<?> field, boolean distinct) {
        super(
                distinct,
                COUNT,
                YdbTypes.UINT64,
                field
        );
    }
}
