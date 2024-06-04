package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.*;

public final class Nothing<T> extends AbstractYdbFunction<T> {

    private static final Name NOTHING = systemName("Nothing");

    private final DataType<T> type;

    public Nothing(DataType<T> type) {
        super(
                NOTHING,
                type
        );

        this.type = type;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(NOTHING, getDataType(), inline(type.getTypeName())));
    }
}
