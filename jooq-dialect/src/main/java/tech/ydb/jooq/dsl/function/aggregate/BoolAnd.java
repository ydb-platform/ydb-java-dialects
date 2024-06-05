package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class BoolAnd extends AbstractYdbAggregateFunction<Boolean> {

    private static final Name BOOL_AND = systemName("bool_and");

    public BoolAnd(Field<Boolean> field) {
        super(
                false,
                BOOL_AND,
                YdbTypes.BOOL,
                field
        );
    }
}
