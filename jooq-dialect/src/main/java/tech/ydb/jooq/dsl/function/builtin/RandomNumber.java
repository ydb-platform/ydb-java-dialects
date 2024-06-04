package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import org.jooq.types.ULong;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class RandomNumber extends AbstractYdbFunction<ULong> {

    private static final Name RANDOM_NUMBER = systemName("RandomNumber");

    private final Field<?>[] fields;

    public RandomNumber(Field<?>[] fields) {
        super(
                RANDOM_NUMBER,
                YdbTypes.UINT64
        );

        this.fields = fields;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(RANDOM_NUMBER, getDataType(), fields));
    }
}
