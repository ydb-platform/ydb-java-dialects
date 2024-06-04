package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;
import tech.ydb.jooq.YdbTypes;

import static org.jooq.impl.DSL.systemName;

public final class BoolOr extends AbstractYdbAggregateFunction<Boolean> {

    private static final Name BOOL_OR = systemName("bool_or");

    public BoolOr(Field<Boolean> field) {
        super(
                false,
                BOOL_OR,
                YdbTypes.BOOL,
                field
        );
    }
}
