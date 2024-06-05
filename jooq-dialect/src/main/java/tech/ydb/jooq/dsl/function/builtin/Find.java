package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import org.jooq.types.UInteger;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class Find<T> extends AbstractYdbFunction<UInteger> {

    private static final Name FIND = systemName("find");

    private final Field<T> source;
    private final Field<T> substring;
    private final Field<UInteger> startingPosition;

    public Find(Field<T> source, Field<T> substring, Field<UInteger> startingPosition) {
        super(
                FIND,
                YdbTypes.UINT32
        );

        this.source = source;
        this.substring = substring;
        this.startingPosition = startingPosition;
    }

    @Override
    public void accept(Context<?> ctx) {
        if (startingPosition != null) {
            ctx.visit(function(FIND, getDataType(), source, substring, startingPosition));
        } else {
            ctx.visit(function(FIND, getDataType(), source, substring));
        }
    }
}