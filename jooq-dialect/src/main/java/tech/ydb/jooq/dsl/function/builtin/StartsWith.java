package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbCondition;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class StartsWith extends AbstractYdbCondition {

    private static final Name STARTS_WITH = systemName("StartsWith");

    private final Field<?> source;
    private final Field<?> substring;

    public StartsWith(Field<?> source, Field<?> substring) {
        this.source = source;
        this.substring = substring;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(STARTS_WITH, getDataType(), source, substring));
    }
}