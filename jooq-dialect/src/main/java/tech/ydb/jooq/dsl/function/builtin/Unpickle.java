package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.*;

public final class Unpickle<T> extends AbstractYdbFunction<T> {

    private static final Name UNPICKLE = systemName("Unpickle");

    private final DataType<T> type;
    private final Field<byte[]> bytes;

    public Unpickle(DataType<T> type, Field<byte[]> bytes) {
        super(
                UNPICKLE,
                type
        );

        this.type = type;
        this.bytes = bytes;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(UNPICKLE, getDataType(), inline(type.getTypeName()), bytes));
    }
}
