package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class MinBy<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name MIN_BY = systemName("min_by");

    public MinBy(Field<T> field, Field<?> cmp) {
        super(
                false,
                MIN_BY,
                field.getDataType(),
                field,
                cmp
        );
    }
}
