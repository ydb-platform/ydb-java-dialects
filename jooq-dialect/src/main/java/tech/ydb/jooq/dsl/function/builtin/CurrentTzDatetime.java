package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import tech.ydb.jooq.YdbTypes;

import java.time.ZonedDateTime;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;
import static org.jooq.impl.YdbTools.combine;

public final class CurrentTzDatetime extends AbstractYdbFunction<ZonedDateTime> {

    private static final Name CURRENT_TZ_DATETIME = systemName("CurrentTzDatetime");

    private final Field<byte[]> timeZone;
    private final Field<?>[] fields;

    public CurrentTzDatetime(Field<byte[]> timeZone, Field<?>[] fields) {
        super(
                CURRENT_TZ_DATETIME,
                YdbTypes.TZ_DATETIME
        );

        this.timeZone = timeZone;
        this.fields = fields;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(CURRENT_TZ_DATETIME, getDataType(), combine(timeZone, fields)));
    }
}


