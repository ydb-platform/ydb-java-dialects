package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class Abs<T extends Number> extends AbstractYdbFunction<T> {

    private static final Name ABS = systemName("Abs");

    private final Field<T> value;

    public Abs(Field<T> value) {
        super(
                ABS,
                value.getDataType()
        );

        this.value = value;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(ABS, getDataType(), value));
    }
}

