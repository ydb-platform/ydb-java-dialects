package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import tech.ydb.jooq.YdbTypes;

import java.time.Instant;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class CurrentUtcTimestamp extends AbstractYdbFunction<Instant> {

    private static final Name CURRENT_UTC_TIMESTAMP = systemName("CurrentUtcTimestamp");

    private final Field<?>[] fields;

    public CurrentUtcTimestamp(Field<?>[] fields) {
        super(
                CURRENT_UTC_TIMESTAMP,
                YdbTypes.TIMESTAMP
        );

        this.fields = fields;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(CURRENT_UTC_TIMESTAMP, getDataType(), fields));
    }
}
