package org.jooq.impl;

import org.jooq.Configuration;
import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Keyword;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.Table;
import tech.ydb.jooq.ReplaceQuery;
import tech.ydb.jooq.UpsertQuery;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.selectFrom;
import static org.jooq.impl.FieldMapsForUpsertReplace.toSQLUpsertSelect;
import static org.jooq.impl.Keywords.K_INTO;
import static org.jooq.impl.Tools.BooleanDataKey.DATA_INSERT_SELECT;
import static org.jooq.impl.Tools.BooleanDataKey.DATA_INSERT_SELECT_WITHOUT_INSERT_COLUMN_LIST;

public class UpsertReplaceQueryImpl<R extends Record>
        extends
        AbstractStoreQuery<R, Field<?>, Field<?>>
        implements
        UpsertQuery<R>,
        ReplaceQuery<R> {

    private final FieldMapsForUpsertReplace upsertReplaceMaps;
    private final Keyword keyword;
    private Select<?> select;

    public UpsertReplaceQueryImpl(Configuration configuration, Table<R> into, Keyword keyword) {
        super(configuration, null, into);

        this.upsertReplaceMaps = new FieldMapsForUpsertReplace(into);
        this.keyword = keyword;
    }

    @Override
    public void newRecord() {
        upsertReplaceMaps.newRecord();
    }

    @Override
    protected Map<Field<?>, Field<?>> getValues() {
        return upsertReplaceMaps.lastMap();
    }

    @Override
    public void addRecord(R record) {
        newRecord();
        setRecord(record);
    }

    @Override
    public void setSelect(Field<?>[] f, Select<?> s) {
        setSelect(Arrays.asList(f), s);
    }

    @Override
    public void setSelect(Collection<? extends Field<?>> f, Select<?> s) {
        upsertReplaceMaps.clear();
        upsertReplaceMaps.addFields(f);
        select = s;
    }

    @Override
    public void addValues(Map<?, ?> map) {
        upsertReplaceMaps.set(map);
    }

    @Override
    public void accept(Context<?> ctx) {
        ctx.scopeStart(this);

        Table<?> t = InlineDerivedTable.inlineDerivedTable(ctx, table(ctx));
        if (t instanceof InlineDerivedTable<?> i) {
            try (var copyUpsertReplaceQuery = copy(
                    d -> {
                        if (!d.upsertReplaceMaps.isEmpty()) {
                            Table<?> m = DSL.table(name("t"));

                            d.select =
                                    selectFrom(
                                            (d.select != null ? d.select : d.upsertReplaceMaps.upsertSelect())
                                                    .asTable(m, d.upsertReplaceMaps.keysFlattened()))
                                            .where(CustomCondition.of(c1 -> c1
                                                    .scopeRegister(i.table, false, m)
                                                    .visit(i.condition)
                                                    .scopeRegister(i.table, false, null)
                                            ));
                        }
                    },
                    i.table
            )) {
                copyUpsertReplaceQuery.accept0(ctx);
            }
        } else {
            accept0(ctx);
        }

        ctx.scopeEnd();
    }

    @Override
    void accept1(Context<?> ctx) {
        toSQLUpsert(ctx);

        toSQLReturning(ctx);
    }

    private void toSQLUpsert(Context<?> ctx) {
        ctx.visit(keyword)
                .sql(' ');

        ctx.visit(K_INTO)
                .sql(' ')
                .declareTables(true, c -> {
                    Table<?> t = table(c);

                    c.visit(t);
                });

        upsertReplaceMaps.toSQLReferenceKeys(ctx);

        if (select != null) {
            Set<Field<?>> keysFlattened = upsertReplaceMaps.keysFlattened();
            if (keysFlattened.isEmpty()) {
                ctx.data(DATA_INSERT_SELECT_WITHOUT_INSERT_COLUMN_LIST, true);
            }

            ctx.data(DATA_INSERT_SELECT, true);

            Select<?> s = select;
            toSQLUpsertSelect(ctx, s);
            ctx.data().remove(DATA_INSERT_SELECT_WITHOUT_INSERT_COLUMN_LIST);
            ctx.data().remove(DATA_INSERT_SELECT);
        } else {
            ctx.visit(upsertReplaceMaps);
        }
    }

    @Override
    public boolean isExecutable() {
        return upsertReplaceMaps.isExecutable() || select != null;
    }

    private UpsertReplaceQueryImpl<R> copy(Consumer<? super UpsertReplaceQueryImpl<R>> finisher) {
        return copy(finisher, table);
    }

    private <O extends Record> UpsertReplaceQueryImpl<O> copy(Consumer<? super UpsertReplaceQueryImpl<O>> finisher, Table<O> t) {
        UpsertReplaceQueryImpl<O> query = new UpsertReplaceQueryImpl<>(configuration(), t, keyword);

        query.upsertReplaceMaps.from(upsertReplaceMaps);
        query.select = select;

        finisher.accept(query);
        return query;
    }
}
