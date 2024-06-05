package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class PopulationVariance extends AbstractYdbAggregateFunction<Double> {

    private static final Name POPULATION_VARIANCE = systemName("population_variance");

    public PopulationVariance(Field<Double> field, boolean distinct) {
        super(
                distinct,
                POPULATION_VARIANCE,
                YdbTypes.DOUBLE,
                field
        );
    }
}
