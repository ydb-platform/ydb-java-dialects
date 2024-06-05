package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbFunction;
import org.jooq.impl.SQLDataType;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class TableRow extends AbstractYdbFunction<Object> {

    private static final Name TABLE_ROW = systemName("TableRow");

    public TableRow() {
        super(
                TABLE_ROW,
                SQLDataType.OTHER
        );
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(TABLE_ROW, getDataType()));
    }
}
