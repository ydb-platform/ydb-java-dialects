package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class Avg<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name AVG = systemName("Avg");

    public Avg(Field<?> field, boolean distinct, DataType<T> type) {
        super(
                distinct,
                AVG,
                type,
                field
        );
    }
}
