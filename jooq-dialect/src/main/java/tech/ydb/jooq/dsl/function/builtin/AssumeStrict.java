package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class AssumeStrict<T> extends AbstractYdbFunction<T> {

    private static final Name ASSUME_STRICT = systemName("AssumeStrict");

    private final Field<T> value;

    public AssumeStrict(Field<T> value) {
        super(
                ASSUME_STRICT,
                value.getDataType()
        );

        this.value = value;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(ASSUME_STRICT, getDataType(), value));
    }
}

