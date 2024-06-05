package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class VarianceSample extends AbstractYdbAggregateFunction<Double> {

    private static final Name VARIANCE_SAMPLE = systemName("variance_sample");

    public VarianceSample(Field<Double> field, boolean distinct) {
        super(
                distinct,
                VARIANCE_SAMPLE,
                YdbTypes.DOUBLE,
                field
        );
    }
}
