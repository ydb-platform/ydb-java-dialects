package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class StdDevSample extends AbstractYdbAggregateFunction<Double> {

    private static final Name STDDEV_SAMPLE = systemName("stddev_sample");

    public StdDevSample(Field<Double> field, boolean distinct) {
        super(
                distinct,
                STDDEV_SAMPLE,
                YdbTypes.DOUBLE,
                field
        );
    }
}
