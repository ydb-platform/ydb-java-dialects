package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class Just<T> extends AbstractYdbFunction<T> {

    private static final Name JUST = systemName("Just");

    private final Field<T> value;

    public Just(Field<T> value) {
        super(
                JUST,
                value.getDataType()
        );

        this.value = value;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(JUST, getDataType(), value));
    }
}

