package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import org.jooq.types.UInteger;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class Substring extends AbstractYdbFunction<byte[]> {

    private static final Name SUBSTRING = systemName("substring");

    private final Field<byte[]> source;
    private final Field<UInteger> startingPosition;
    private final Field<UInteger> length;

    public Substring(Field<byte[]> source, Field<UInteger> startingPosition, Field<UInteger> length) {
        super(
                SUBSTRING,
                YdbTypes.STRING
        );

        this.source = source;
        this.startingPosition = startingPosition;
        this.length = length;
    }

    @Override
    public void accept(Context<?> ctx) {
        if (length != null) {
            ctx.visit(function(SUBSTRING, getDataType(), source, startingPosition, length));
        } else {
            ctx.visit(function(SUBSTRING, getDataType(), source, startingPosition));
        }
    }
}
