package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Condition;
import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.*;

public final class Ensure<T> extends AbstractYdbFunction<T> {

    private static final Name ENSURE = systemName("Ensure");

    private final Field<T> value;
    private final Condition condition;
    private final Field<byte[]> message;

    public Ensure(Field<T> value, Condition condition, Field<byte[]> message) {
        super(
                ENSURE,
                value.getDataType()
        );

        this.value = value;
        this.condition = condition;
        this.message = message;
    }

    @Override
    public void accept(Context<?> ctx) {
        if (message != null) {
            ctx.visit(function(ENSURE, getDataType(), value, condition, message));
        } else {
            ctx.visit(function(ENSURE, getDataType(), value, condition));
        }
    }
}
