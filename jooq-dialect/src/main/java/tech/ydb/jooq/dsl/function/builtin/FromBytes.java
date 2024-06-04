package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.systemName;

public final class FromBytes<T> extends AbstractYdbFunction<T> {

    private static final Name FROM_BYTES = systemName("FromBytes");

    private final Field<byte[]> bytes;
    private final DataType<T> type;

    public FromBytes(Field<byte[]> bytes, DataType<T> type) {
        super(
                FROM_BYTES,
                type
        );

        this.bytes = bytes;
        this.type = type;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(FROM_BYTES, getDataType(), bytes, inline(type.getTypeName())));
    }
}
