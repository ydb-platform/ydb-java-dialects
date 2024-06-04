package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class SumIf<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name SUM_IF = systemName("sum_if");

    public SumIf(Field<?> field, Condition condition, boolean distinct, DataType<T> type) {
        super(
                distinct,
                SUM_IF,
                type,
                field,
                condition
        );
    }
}

