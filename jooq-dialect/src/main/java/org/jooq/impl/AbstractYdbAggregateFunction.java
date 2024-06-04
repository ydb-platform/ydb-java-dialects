package org.jooq.impl;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Name;

public abstract class AbstractYdbAggregateFunction<T> extends AbstractAggregateFunction<T> {
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
}
