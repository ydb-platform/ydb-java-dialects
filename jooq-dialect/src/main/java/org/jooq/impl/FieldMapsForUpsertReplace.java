package org.jooq.impl;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.IntStream;
import org.jooq.Context;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RenderContext.CastMode;
import org.jooq.Select;
import org.jooq.Table;
import static org.jooq.impl.Keywords.K_VALUES;
import static org.jooq.impl.Tools.BooleanDataKey.DATA_STORE_ASSIGNMENT;


public final class FieldMapsForUpsertReplace extends AbstractQueryPart {
    private final Table<?> table;
    private final Map<Field<?>, Field<?>> empty;
    private final Map<Field<?>, List<Field<?>>> values;
    private int rows;
    private int nextRow = -1;

    public FieldMapsForUpsertReplace(Table<?> table) {
        this.table = table;
        this.values = new LinkedHashMap<>();
        this.empty = new LinkedHashMap<>();
    }

    public void clear() {
        empty.clear();
        values.clear();
        rows = 0;
        nextRow = -1;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public void from(FieldMapsForUpsertReplace i) {
        empty.putAll(i.empty);

        for (Entry<Field<?>, List<Field<?>>> e : i.values.entrySet()) {
            values.put(e.getKey(), new ArrayList<>(e.getValue()));
        }

        rows = i.rows;
        nextRow = i.nextRow;
    }

    @Override
    public void accept(Context<?> ctx) {
        toSQLValues(ctx);
    }

    private void toSQLValues(Context<?> ctx) {
        ctx.formatSeparator()
                .visit(K_VALUES)
                .sql(' ');
        toSQL92Values(ctx);
    }

    public static void toSQLUpsertSelect(Context<?> ctx, Select<?> select) {
        ctx.formatSeparator().visit(select);
    }

    public Select<Record> upsertSelect() {
        Map<Field<?>, List<Field<?>>> v = valuesFlattened();

        return IntStream.range(0, rows)
                .mapToObj(row -> (Select<Record>) DSL.select(Tools.map(v.entrySet(),
                        e -> patchDefault0(e.getValue().get(row), e.getKey()))))
                .reduce(Select::unionAll)
                .orElse(null);
    }

    private void toSQL92Values(Context<?> ctx) {
        boolean indent = values.size() > 1;

        CastMode previous = ctx.castMode();
        ctx.castMode(CastMode.NEVER);

        for (int row = 0; row < rows; row++) {
            if (row > 0) {
                ctx.sql(", ");
            }

            ctx.sql('(');

            if (indent) {
                ctx.formatIndentStart();
            }

            String separator = "";
            for (Entry<Field<?>, List<Field<?>>> e : valuesFlattened().entrySet()) {
                List<Field<?>> list = e.getValue();
                ctx.sql(separator);

                if (indent) {
                    ctx.formatNewLine();
                }

                ctx.visit(patchDefault0(list.get(row), e.getKey()));
                separator = ", ";
            }

            if (indent) {
                ctx.formatIndentEnd()
                        .formatNewLine();
            }

            ctx.sql(')');
        }

        ctx.castMode(previous);
    }

    private static Field<?> patchDefault0(Field<?> d, Field<?> f) {
        if (d instanceof Default) {
            return Tools.orElse(f.getDataType().default_(), () -> DSL.inline(null, f));
        }

        return d;
    }


    public void addFields(Collection<?> fields) {
        if (rows == 0) {
            newRecord();
        }

        initNextRow();

        for (Object field : fields) {
            Field<?> f = Tools.tableField(table, field);
            Field<?> e = empty.computeIfAbsent(f, LazyVal::new);

            values.computeIfAbsent(f, k -> rows > 0
                    ? new ArrayList<>(Collections.nCopies(rows, e))
                    : new ArrayList<>());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Field<T> set(Field<T> field, Field<T> value) {
        addFields(Collections.singletonList(field));
        return (Field<T>) values.get(field).set(rows - 1, value);
    }

    public void set(Map<?, ?> map) {
        addFields(map.keySet());

        for (Entry<?, ?> entry : map.entrySet()) {
            Object k = entry.getKey();
            Object v = entry.getValue();
            Field<?> field = Tools.tableField(table, k);
            values.get(field).set(rows - 1, Tools.field(v, field));
        }
    }

    private void initNextRow() {
        if (rows == nextRow) {
            Iterator<List<Field<?>>> v = values.values().iterator();
            Iterator<Field<?>> e = empty.values().iterator();

            while (v.hasNext() && e.hasNext()) {
                v.next().add(e.next());
            }

            rows++;
        }
    }

    public void newRecord() {
        if (nextRow < rows) {
            nextRow++;
        }
    }

    private Map<Field<?>, Field<?>> map(final int index) {
        return new AbstractMap<>() {
            private transient Set<Entry<Field<?>, Field<?>>> entrySet;

            @Override
            public Set<Entry<Field<?>, Field<?>>> entrySet() {
                if (entrySet == null) {
                    entrySet = new EntrySet();
                }

                return entrySet;
            }

            @Override
            public boolean containsKey(Object key) {
                return values.containsKey(key);
            }

            @Override
            public boolean containsValue(Object value) {
                return values.values().stream().anyMatch(list -> list.get(index).equals(value));
            }

            @Override
            public Field<?> get(Object key) {
                List<Field<?>> list = values.get(key);
                return list == null ? null : list.get(index);
            }

            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public Field<?> put(Field<?> key, Field<?> value) {
                return FieldMapsForUpsertReplace.this.set(key, (Field) value);
            }

            @Override
            public Field<?> remove(Object key) {
                List<Field<?>> list = values.remove(key);
                return list == null ? null : list.get(index);
            }

            @Override
            public Set<Field<?>> keySet() {
                return values.keySet();
            }

            private final class EntrySet extends AbstractSet<Entry<Field<?>, Field<?>>> {
                @Override
                public int size() {
                    return values.size();
                }

                @Override
                public void clear() {
                    values.clear();
                }

                @Override
                public Iterator<Entry<Field<?>, Field<?>>> iterator() {
                    return new Iterator<>() {
                        final Iterator<Entry<Field<?>, List<Field<?>>>> delegate = values.entrySet().iterator();

                        @Override
                        public boolean hasNext() {
                            return delegate.hasNext();
                        }

                        @Override
                        public Entry<Field<?>, Field<?>> next() {
                            Entry<Field<?>, List<Field<?>>> entry = delegate.next();
                            return new SimpleImmutableEntry<>(entry.getKey(), entry.getValue().get(index));
                        }

                        @Override
                        public void remove() {
                            delegate.remove();
                        }
                    };
                }
            }
        };
    }

    public Map<Field<?>, Field<?>> lastMap() {
        return map(rows - 1);
    }

    public boolean isExecutable() {
        return rows > 0;
    }

    public Set<Field<?>> toSQLReferenceKeys(Context<?> ctx) {
        if (values.keySet().stream().allMatch(AbstractStoreQuery.UnknownField.class::isInstance)) {
            return Collections.emptySet();
        }

        Set<Field<?>> fields = keysFlattened();

        if (!fields.isEmpty()) {
            ctx.data(DATA_STORE_ASSIGNMENT, true, c -> c.sql(" (").visit(QueryPartCollectionView.wrap(fields).qualify(false)).sql(')'));
        }

        return fields;
    }

    public Set<Field<?>> keysFlattened() {
        return valuesFlattened().keySet();
    }

    private Map<Field<?>, List<Field<?>>> valuesFlattened() {
        Map<Field<?>, List<Field<?>>> result = new LinkedHashMap<>();

        for (Entry<Field<?>, List<Field<?>>> entry : values.entrySet()) {
            Field<?> key = entry.getKey();
            DataType<?> keyType = key.getDataType();
            List<Field<?>> value = entry.getValue();

            if (keyType.isEmbeddable()) {
                List<Iterator<? extends Field<?>>> valueFlattened = new ArrayList<>(value.size());

                for (Field<?> v : value) {
                    valueFlattened.add(Tools.flatten(v).iterator());
                }

                for (Field<?> k : Tools.flatten(key)) {
                    List<Field<?>> list = new ArrayList<>(value.size());

                    for (Iterator<? extends Field<?>> v : valueFlattened) {
                        list.add(v.hasNext() ? v.next() : null);
                    }

                    result.put(k, list);
                }
            } else {
                result.put(key, value);
            }
        }

        return result;
    }
}
