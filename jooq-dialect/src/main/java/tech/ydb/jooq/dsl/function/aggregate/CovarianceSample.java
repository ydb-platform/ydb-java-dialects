package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class CovarianceSample extends AbstractYdbAggregateFunction<Double> {

    private static final Name COVARIANCE_SAMPLE = systemName("covariance_sample");

    public CovarianceSample(Field<Double> field1, Field<Double> field2, boolean distinct) {
        super(
                distinct,
                COVARIANCE_SAMPLE,
                YdbTypes.DOUBLE,
                field1,
                field2
        );
    }
}
