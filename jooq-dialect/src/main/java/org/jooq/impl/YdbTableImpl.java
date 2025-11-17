package org.jooq.impl;

import org.jooq.Comment;
import org.jooq.Condition;
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
        return new HintedTable<>(this, DSL.keyword("use primary key"), new QueryPartList<>(PRIMARY_KEY));
    }
}
