package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import tech.ydb.jooq.YdbTypes;

import java.time.LocalDateTime;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class CurrentUtcDatetime extends AbstractYdbFunction<LocalDateTime> {

    private static final Name CURRENT_UTC_DATETIME = systemName("CurrentUtcDatetime");

    private final Field<?>[] fields;

    public CurrentUtcDatetime(Field<?>[] fields) {
        super(
                CURRENT_UTC_DATETIME,
                YdbTypes.DATETIME
        );

        this.fields = fields;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(CURRENT_UTC_DATETIME, getDataType(), fields));
    }
}