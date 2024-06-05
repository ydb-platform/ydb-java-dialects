package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class Correlation extends AbstractYdbAggregateFunction<Double> {

    private static final Name CORRELATION = systemName("correlation");

    public Correlation(Field<Double> field1, Field<Double> field2, boolean distinct) {
        super(
                distinct,
                CORRELATION,
                YdbTypes.DOUBLE,
                field1,
                field2
        );
    }
}
