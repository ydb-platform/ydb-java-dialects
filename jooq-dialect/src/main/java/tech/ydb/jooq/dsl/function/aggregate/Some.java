package tech.ydb.jooq.dsl.function.aggregate;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.AbstractYdbAggregateFunction;

import static org.jooq.impl.DSL.systemName;

public final class Some<T> extends AbstractYdbAggregateFunction<T> {

    private static final Name SOME = systemName("some");

    public Some(Field<T> field, boolean distinct) {
        super(
                distinct,
                SOME,
                field.getDataType(),
                field
        );
    }
}
