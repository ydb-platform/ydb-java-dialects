package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class Percentile<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name PERCENTILE = systemName("percentile");

    public Percentile(Field<T> field, Field<Double> percent, boolean distinct) {
        super(
                distinct,
                PERCENTILE,
                field.getDataType(),
                field,
                percent
        );
    }
}
