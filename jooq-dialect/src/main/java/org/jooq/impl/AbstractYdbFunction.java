package org.jooq.impl;

import org.jooq.Binding;
import org.jooq.Comment;
import org.jooq.Context;
import org.jooq.DataType;
import org.jooq.Name;

public abstract class AbstractYdbFunction<T> extends AbstractField<T> {
    protected AbstractYdbFunction(Name name, DataType<T> type) {
        super(name, type);
    }

    protected AbstractYdbFunction(Name name, DataType<T> type, Comment comment, Binding<?, T> binding) {
        super(name, type, comment, binding);
    }

    @Override
    protected boolean parenthesised(Context<?> ctx) {
        return true;
    }
}
