package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbCondition;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class EndsWith extends AbstractYdbCondition {

    private static final Name ENDS_WITH = systemName("EndsWith");

    private final Field<?> source;
    private final Field<?> substring;

    public EndsWith(Field<?> source, Field<?> substring) {
        this.source = source;
        this.substring = substring;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(ENDS_WITH, getDataType(), source, substring));
    }
}
