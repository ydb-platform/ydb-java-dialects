package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import tech.ydb.jooq.YdbTypes;

import java.time.ZonedDateTime;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class AddTimezone extends AbstractYdbFunction<ZonedDateTime> {

    private static final Name ADD_TIMEZONE = systemName("AddTimezone");

    private final Field<?> date;
    private final Field<byte[]> timeZone;

    public AddTimezone(Field<?> date, Field<byte[]> timeZone) {
        super(
                ADD_TIMEZONE,
                YdbTypes.TZ_DATE
        );

        this.date = date;
        this.timeZone = timeZone;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(ADD_TIMEZONE, getDataType(), date, timeZone));
    }
}
