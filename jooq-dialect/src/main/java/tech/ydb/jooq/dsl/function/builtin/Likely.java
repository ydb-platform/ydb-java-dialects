package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Condition;
import org.jooq.Context;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbCondition;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class Likely extends AbstractYdbCondition {

    private static final Name LIKELY = systemName("Likely");

    private final Condition condition;

    public Likely(Condition condition) {
        this.condition = condition;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(LIKELY, getDataType(), condition));
    }
}

