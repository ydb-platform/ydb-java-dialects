package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class Min<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name MIN = systemName("Min");

    public Min(Field<T> field, boolean distinct) {
        super(
                distinct,
                MIN,
                field.getDataType(),
                field
        );
    }
}
