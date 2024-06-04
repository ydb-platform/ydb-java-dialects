package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import org.jooq.types.UByte;
import org.jooq.types.UNumber;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class ClearBit<T extends UNumber> extends AbstractYdbFunction<T> {

    private static final Name CLEAR_BIT = systemName("ClearBit");

    private final Field<T> value;
    private final Field<UByte> index;

    public ClearBit(Field<T> value, Field<UByte> index) {
        super(
                CLEAR_BIT,
                value.getDataType()
        );

        this.value = value;
        this.index = index;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(CLEAR_BIT, getDataType(), value, index));
    }
}

