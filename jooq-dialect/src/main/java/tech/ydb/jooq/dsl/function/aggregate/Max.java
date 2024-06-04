package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class Max<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name MAX = systemName("Max");

    public Max(Field<T> field, boolean distinct) {
        super(
                distinct,
                MAX,
                field.getDataType(),
                field
        );
    }
}
