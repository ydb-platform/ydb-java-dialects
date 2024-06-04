package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class Unwrap<T> extends AbstractYdbFunction<T> {

    private static final Name UNWRAP = systemName("Unwrap");

    private final Field<T> value;
    private final Field<?> message;

    public Unwrap(Field<T> value, Field<?> message) {
        super(
                UNWRAP,
                value.getDataType()
        );

        this.value = value;
        this.message = message;
    }

    @Override
    public void accept(Context<?> ctx) {
        if (message != null) {
            ctx.visit(function(UNWRAP, getDataType(), value, message));
        } else {
            ctx.visit(function(UNWRAP, getDataType(), value));
        }
    }
}

