package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class Sum<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name SUM = systemName("Sum");

    public Sum(Field<?> field, boolean distinct, DataType<T> type) {
        super(
                distinct,
                SUM,
                type,
                field
        );
    }
}
