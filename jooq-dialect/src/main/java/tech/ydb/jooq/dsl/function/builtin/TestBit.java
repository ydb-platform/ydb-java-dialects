package tech.ydb.jooq.dsl.function.builtin;

import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbCondition;
import org.jooq.types.UByte;
import org.jooq.types.UNumber;

import static org.jooq.impl.DSL.function;
import static org.jooq.impl.DSL.systemName;

public final class TestBit extends AbstractYdbCondition {

    private static final Name TEST_BIT = systemName("TestBit");

    private final Field<? extends UNumber> source;
    private final Field<UByte> index;

    public TestBit(Field<? extends UNumber> source, Field<UByte> index) {
        this.source = source;
        this.index = index;
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.visit(function(TEST_BIT, getDataType(), source, index));
    }
}