package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class Median<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name MEDIAN = systemName("median");

    public Median(Field<T> field, Field<Double> percent, boolean distinct) {
        super(
                distinct,
                MEDIAN,
                field.getDataType(),
                percent != null ? new Field[]{field, percent} : new Field[]{field}
        );
    }
}
