package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Condition;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import org.jooq.types.ULong;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class CountIf extends AbstractYdbAggregateFunction<ULong> {

    private static final Name COUNT_IF = systemName("count_if");

    public CountIf(Condition field) {
        super(
                false,
                COUNT_IF,
                YdbTypes.UINT64,
                field
        );
    }
}

