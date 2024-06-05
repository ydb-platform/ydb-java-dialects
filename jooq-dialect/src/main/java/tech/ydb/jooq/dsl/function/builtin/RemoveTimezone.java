package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class RemoveTimezone<T> extends AbstractYdbFunction<T> {

    private static final Name REMOVE_TIMEZONE = systemName("RemoveTimezone");

    private final Field<?> date;

    public RemoveTimezone(Field<?> date, DataType<T> type) {
        super(
                REMOVE_TIMEZONE,
                type
        );

        this.date = date;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(REMOVE_TIMEZONE, getDataType(), date));
    }
}
