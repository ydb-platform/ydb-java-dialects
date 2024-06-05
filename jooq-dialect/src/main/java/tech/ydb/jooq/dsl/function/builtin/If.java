package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Condition;
import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class If<T> extends AbstractYdbFunction<T> {

    private static final Name IF = systemName("if");

    private final Condition condition;
    private final Field<T> ifTrue;
    private final Field<T> ifFalse;

    public If(Condition condition, Field<T> ifTrue, Field<T> ifFalse) {
        super(
                IF,
                ifTrue.getDataType()
        );

        this.condition = condition;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
    }

    @Override
    public void accept(Context<?> ctx) {
        if (ifFalse != null) {
            ctx.visit(function(IF, getDataType(), condition, ifTrue, ifFalse));
        } else {
            ctx.visit(function(IF, getDataType(), condition, ifTrue));
        }
    }
}