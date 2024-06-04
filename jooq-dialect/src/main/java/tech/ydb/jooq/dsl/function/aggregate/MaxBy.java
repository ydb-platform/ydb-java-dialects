package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class MaxBy<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name MAX_BY = systemName("max_by");

    public MaxBy(Field<T> field, Field<?> cmp) {
        super(
                false,
                MAX_BY,
                field.getDataType(),
                field,
                cmp
        );
    }
}
