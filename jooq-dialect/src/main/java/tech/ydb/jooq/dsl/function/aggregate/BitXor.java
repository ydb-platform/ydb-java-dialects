package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class BitXor<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name BIT_XOR = systemName("bit_xor");

    public BitXor(Field<T> field) {
        super(
                false,
                BIT_XOR,
                field.getDataType(),
                field
        );
    }
}
