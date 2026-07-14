package org.jooq.impl;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;

import java.util.function.Function;

import static org.jooq.impl.Tools.EMPTY_FIELD;

public abstract class AbstractYdbAggregateFunction<T> extends AbstractAggregateFunction<T, AbstractYdbAggregateFunction<T>> {
    protected AbstractYdbAggregateFunction(String name, DataType<T> type, Field<?>... arguments) {
        super(name, type, arguments);
    }

    protected AbstractYdbAggregateFunction(Name name, DataType<T> type, Field<?>... arguments) {
        super(name, type, arguments);
    }

    protected AbstractYdbAggregateFunction(boolean distinct, String name, DataType<T> type, Field<?>... arguments) {
        super(distinct, name, type, arguments);
    }

    protected AbstractYdbAggregateFunction(boolean distinct, Name name, DataType<T> type, Field<?>... arguments) {
        super(distinct, name, type, arguments);
    }

    @Override
    AbstractYdbAggregateFunction<T> copyAggregateFunction(
            Function<? super AbstractYdbAggregateFunction<T>, ? extends AbstractYdbAggregateFunction<T>> function) {
        return function.apply(new Copy<>(distinct, getQualifiedName(), getDataType(), getArguments().toArray(EMPTY_FIELD)));
    }

    private static final class Copy<T> extends AbstractYdbAggregateFunction<T> {
        private Copy(boolean distinct, Name name, DataType<T> type, Field<?>... arguments) {
            super(distinct, name, type, arguments);
        }
    }
}
