package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class NaNvl<T> extends AbstractYdbFunction<T> {

    private static final Name NANVL = systemName("nanvl");

    private final Field<T> expression;
    private final Field<T> replacement;

    public NaNvl(Field<T> expression, Field<T> replacement) {
        super(
                NANVL,
                expression.getDataType()
        );

        this.expression = expression;
        this.replacement = replacement;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(NANVL, getDataType(), expression, replacement));
    }
}