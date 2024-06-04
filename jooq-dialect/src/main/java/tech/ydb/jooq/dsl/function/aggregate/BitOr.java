package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class BitOr<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name BIT_OR = systemName("bit_or");

    public BitOr(Field<T> field) {
        super(
                false,
                BIT_OR,
                field.getDataType(),
                field
        );
    }
}
