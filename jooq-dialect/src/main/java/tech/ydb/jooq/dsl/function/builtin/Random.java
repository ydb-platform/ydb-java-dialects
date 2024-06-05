package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class Random extends AbstractYdbFunction<Double> {

    private static final Name RANDOM = systemName("Random");

    private final Field<?>[] fields;

    public Random(Field<?>[] fields) {
        super(
                RANDOM,
                YdbTypes.DOUBLE
        );

        this.fields = fields;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(RANDOM, getDataType(), fields));
    }
}

