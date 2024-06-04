package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class AvgIf extends AbstractYdbAggregateFunction<Double> {

    private static final Name AVG_IF = systemName("avg_if");

    public AvgIf(Field<?> field, Condition condition, boolean distinct) {
        super(
                distinct,
                AVG_IF,
                YdbTypes.DOUBLE,
                field,
                condition
        );
    }
}

