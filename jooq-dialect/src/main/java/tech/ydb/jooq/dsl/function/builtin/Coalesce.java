package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class Coalesce<T> extends AbstractYdbFunction<T> {

    private static final Name COALESCE = systemName("coalesce");

    private final Field<T>[] fields;

    public Coalesce(Field<T>[] fields) {
        super(
                COALESCE,
                fields[0].getDataType()
        );

        this.fields = fields;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(COALESCE, getDataType(), fields));
    }
}
