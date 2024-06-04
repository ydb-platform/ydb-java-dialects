package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class PopulationStdDev extends AbstractYdbAggregateFunction<Double> {

    private static final Name POPULATION_STDDEV = systemName("population_stddev");

    public PopulationStdDev(Field<Double> field, boolean distinct) {
        super(
                distinct,
                POPULATION_STDDEV,
                YdbTypes.DOUBLE,
                field
        );
    }
}
