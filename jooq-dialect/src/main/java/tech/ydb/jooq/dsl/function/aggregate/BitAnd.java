package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class BitAnd<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name BIT_AND = systemName("bit_and");

    public BitAnd(Field<T> field) {
        super(
                false,
                BIT_AND,
                field.getDataType(),
                field
        );
    }
}
