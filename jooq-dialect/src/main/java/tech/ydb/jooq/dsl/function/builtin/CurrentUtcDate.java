package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import tech.ydb.jooq.YdbTypes;

import java.time.LocalDate;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class CurrentUtcDate extends AbstractYdbFunction<LocalDate> {

    private static final Name CURRENT_UTC_DATE = systemName("CurrentUtcDate");

    private final Field<?>[] fields;

    public CurrentUtcDate(Field<?>[] fields) {
        super(
                CURRENT_UTC_DATE,
                YdbTypes.DATE
        );

        this.fields = fields;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(CURRENT_UTC_DATE, getDataType(), fields));
    }
}
