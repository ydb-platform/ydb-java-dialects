package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class MaxOf<T> extends AbstractYdbFunction<T> {

    private static final Name MAX_OF = systemName("max_of");

    private final Field<T>[] fields;

    public MaxOf(Field<T>[] fields) {
        super(
                MAX_OF,
                fields[0].getDataType()
        );

        this.fields = fields;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(MAX_OF, getDataType(), fields));
    }
}

