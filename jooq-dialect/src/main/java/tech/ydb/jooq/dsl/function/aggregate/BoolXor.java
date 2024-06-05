package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class BoolXor extends AbstractYdbAggregateFunction<Boolean> {

    private static final Name BOOL_XOR = systemName("bool_xor");

    public BoolXor(Field<Boolean> field) {
        super(
                false,
                BOOL_XOR,
                YdbTypes.BOOL,
                field
        );
    }
}
