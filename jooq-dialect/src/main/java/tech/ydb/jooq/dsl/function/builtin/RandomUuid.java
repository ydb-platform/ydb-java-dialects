package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import tech.ydb.jooq.YdbTypes;

import java.util.UUID;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class RandomUuid extends AbstractYdbFunction<UUID> {

    private static final Name RANDOM_UUID = systemName("RandomUuid");

    private final Field<?>[] fields;

    public RandomUuid(Field<?>[] fields) {
        super(
                RANDOM_UUID,
                YdbTypes.UUID
        );

        this.fields = fields;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(RANDOM_UUID, getDataType(), fields));
    }
}
