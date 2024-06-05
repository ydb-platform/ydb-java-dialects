package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.*;
import org.jooq.impl.AbstractYdbFunction;
import org.jooq.types.UInteger;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class Length extends AbstractYdbFunction<UInteger> {

    private static final Name LENGTH = systemName("length");

    private final Field<?> value;

    public Length(Field<?> value) {
        super(
                LENGTH,
                YdbTypes.UINT32
        );

        this.value = value;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(LENGTH, getDataType(), value));
    }
}
