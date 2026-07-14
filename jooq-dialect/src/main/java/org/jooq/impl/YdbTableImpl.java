package org.jooq.impl;

import org.jooq.Comment;
import org.jooq.Condition;
import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableOptions;

public class YdbTableImpl<R extends Record> extends TableImpl<R> {
    private static final Name PRIMARY_KEY = new UnqualifiedName("PRIMARY KEY", Name.Quoted.SYSTEM);

    protected YdbTableImpl(Name alias, Table<R> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    protected YdbTableImpl(Name name, Schema schema, Table<R> aliased, Field<?>[] parameters, Comment comment, TableOptions options, Condition where) {
        super(name, schema, null, null, null, aliased, parameters, comment, options, where);
    }

    public Table<R> viewPrimaryKey() {
        return new YdbHintedTable<>(this);
    }

    private static final class YdbHintedTable<R extends Record> extends AbstractDelegatingTable<R> {
        private YdbHintedTable(AbstractTable<R> delegate) {
            super(delegate);
        }

        @Override
        <O extends Record> AbstractDelegatingTable<O> construct(AbstractTable<O> newDelegate) {
            return new YdbHintedTable<>(newDelegate);
        }

        @Override
        public void accept(Context<?> ctx) {
            ctx.visit(delegate)
                    .sql(' ').visit(Keywords.K_VIEW)
                    .sql(' ').visit(PRIMARY_KEY);
        }
    }
}
