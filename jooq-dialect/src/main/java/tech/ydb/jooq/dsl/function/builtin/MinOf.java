package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class MinOf<T> extends AbstractYdbFunction<T> {

    private static final Name MIN_OF = systemName("min_of");

    private final Field<T>[] fields;

    public MinOf(Field<T>[] fields) {
        super(
                MIN_OF,
                fields[0].getDataType()
        );

        this.fields = fields;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(MIN_OF, getDataType(), fields));
    }
}

