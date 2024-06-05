package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class ToBytes extends AbstractYdbFunction<byte[]> {

    private static final Name TO_BYTES = systemName("ToBytes");

    private final Field<?> value;

    public ToBytes(Field<?> value) {
        super(
                TO_BYTES,
                YdbTypes.STRING
        );

        this.value = value;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(TO_BYTES, getDataType(), value));
    }
}
