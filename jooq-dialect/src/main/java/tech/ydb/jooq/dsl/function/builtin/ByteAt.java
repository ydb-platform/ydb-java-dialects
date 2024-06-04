package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import org.jooq.types.UByte;
import org.jooq.types.UInteger;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class ByteAt extends AbstractYdbFunction<UByte> {

    private static final Name BYTE_AT = systemName("ByteAt");

    private final Field<?> source;
    private final Field<UInteger> index;

    public ByteAt(Field<?> source, Field<UInteger> index) {
        super(
                BYTE_AT,
                YdbTypes.UINT8
        );

        this.source = source;
        this.index = index;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(BYTE_AT, getDataType(), source, index));
    }
}
