package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class Pickle extends AbstractYdbFunction<byte[]> {

    private static final Name PICKLE = systemName("Pickle");

    private final Field<?> value;

    public Pickle(Field<?> value) {
        super(
                PICKLE,
                YdbTypes.STRING
        );

        this.value = value;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(PICKLE, getDataType(), value));
    }
}
