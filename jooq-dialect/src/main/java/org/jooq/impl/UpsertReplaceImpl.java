package org.jooq.impl;

import org.jooq.Record;
import org.jooq.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract sealed class UpsertReplaceImpl<R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22,
        SELF extends UpsertReplaceImpl>
        extends AbstractDelegatingDMLQuery<R, UpsertReplaceQueryImpl<R>>
        permits
        ReplaceImpl,
        UpsertImpl {

    private final Table<R> into;
    private Field<?>[] fields;

    public UpsertReplaceImpl(Configuration configuration, Table<R> into, Keyword keyword) {
        this(configuration, into, Collections.emptyList(), keyword);
    }

    @SuppressWarnings("resource")
    public UpsertReplaceImpl(Configuration configuration,
                             Table<R> into,
                             Collection<? extends Field<?>> fields,
                             Keyword keyword) {
        super(new UpsertReplaceQueryImpl<>(configuration, into, keyword));

        this.into = into;
        columns(fields);
    }

    public SELF select(Select select) {
        getDelegate().setSelect(fields, select);
        return (SELF) this;
    }

    public SELF values(T1 value1) {
        return values(new Object[]{value1});
    }

    public SELF values(T1 value1, T2 value2) {
        return values(new Object[]{value1, value2});
    }

    public SELF values(T1 value1, T2 value2, T3 value3) {
        return values(new Object[]{value1, value2, value3});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4) {
        return values(new Object[]{value1, value2, value3, value4});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5) {
        return values(new Object[]{value1, value2, value3, value4, value5});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10, T11 value11) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10, T11 value11, T12 value12) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10, T11 value11, T12 value12, T13 value13) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10, T11 value11, T12 value12, T13 value13, T14 value14) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10, T11 value11, T12 value12, T13 value13, T14 value14, T15 value15) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10, T11 value11, T12 value12, T13 value13, T14 value14, T15 value15, T16 value16) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10, T11 value11, T12 value12, T13 value13, T14 value14, T15 value15, T16 value16, T17 value17) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16, value17});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10, T11 value11, T12 value12, T13 value13, T14 value14, T15 value15, T16 value16, T17 value17, T18 value18) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16, value17, value18});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10, T11 value11, T12 value12, T13 value13, T14 value14, T15 value15, T16 value16, T17 value17, T18 value18, T19 value19) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16, value17, value18, value19});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10, T11 value11, T12 value12, T13 value13, T14 value14, T15 value15, T16 value16, T17 value17, T18 value18, T19 value19, T20 value20) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16, value17, value18, value19, value20});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10, T11 value11, T12 value12, T13 value13, T14 value14, T15 value15, T16 value16, T17 value17, T18 value18, T19 value19, T20 value20, T21 value21) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16, value17, value18, value19, value20, value21});
    }

    public SELF values(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6, T7 value7, T8 value8, T9 value9, T10 value10, T11 value11, T12 value12, T13 value13, T14 value14, T15 value15, T16 value16, T17 value17, T18 value18, T19 value19, T20 value20, T21 value21, T22 value22) {
        return values(new Object[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16, value17, value18, value19, value20, value21, value22});
    }

    public SELF values(RowN values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(RowN... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row1<T1> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row1<T1>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row2<T1, T2> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row2<T1, T2>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row3<T1, T2, T3> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row3<T1, T2, T3>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row4<T1, T2, T3, T4> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row4<T1, T2, T3, T4>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row5<T1, T2, T3, T4, T5> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row5<T1, T2, T3, T4, T5>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row6<T1, T2, T3, T4, T5, T6> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row6<T1, T2, T3, T4, T5, T6>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row7<T1, T2, T3, T4, T5, T6, T7> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row7<T1, T2, T3, T4, T5, T6, T7>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row8<T1, T2, T3, T4, T5, T6, T7, T8> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row8<T1, T2, T3, T4, T5, T6, T7, T8>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row9<T1, T2, T3, T4, T5, T6, T7, T8, T9> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row9<T1, T2, T3, T4, T5, T6, T7, T8, T9>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Row22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> values) {
        return values(values.fields());
    }

    public SELF valuesOfRows(Row22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>... values) {
        return valuesOfRows(Arrays.asList(values));
    }

    public SELF values(Record values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record1<T1> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record1<T1>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record2<T1, T2> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record2<T1, T2>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record3<T1, T2, T3> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record3<T1, T2, T3>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record4<T1, T2, T3, T4> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record4<T1, T2, T3, T4>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record5<T1, T2, T3, T4, T5> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record5<T1, T2, T3, T4, T5>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record6<T1, T2, T3, T4, T5, T6> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record6<T1, T2, T3, T4, T5, T6>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record7<T1, T2, T3, T4, T5, T6, T7> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record7<T1, T2, T3, T4, T5, T6, T7>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record8<T1, T2, T3, T4, T5, T6, T7, T8> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record8<T1, T2, T3, T4, T5, T6, T7, T8>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record9<T1, T2, T3, T4, T5, T6, T7, T8, T9>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record21<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    public SELF values(Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> values) {
        return values(values.intoArray());
    }

    public SELF valuesOfRecords(Record22<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>... values) {
        return valuesOfRecords(Arrays.asList(values));
    }

    @SuppressWarnings("resource")
    public SELF valuesOfRows(Collection values) {
        for (Object row : values) {
            values(((Row) row).fields());
        }

        return (SELF) this;
    }

    @SuppressWarnings("resource")
    public SELF valuesOfRecords(Collection values) {
        for (Object record : values) {
            values(((Record) record).intoArray());
        }

        return (SELF) this;
    }

    public SELF values(Object... values) {
        if (!Tools.isEmpty(fields) && fields.length != values.length) {
            throw new IllegalArgumentException("The number of values must match the number of fields");
        }

        getDelegate().newRecord();

        if (Tools.isEmpty(fields)) {
            for (int i = 0; i < values.length; i++) {
                addValue(getDelegate(), null, i, values[i]);
            }
        } else {
            for (int i = 0; i < fields.length; i++) {
                addValue(getDelegate(), fields.length > 0 ? fields[i] : null, i, values[i]);
            }
        }

        return (SELF) this;
    }

    public SELF values(Collection<?> values) {
        return values(values.toArray());
    }

    private <T> void addValue(UpsertReplaceQueryImpl<R> delegate, Field<T> field, int index, Object object) {
        if (object instanceof Field f) {
            delegate.addValue(field, index, f);
        } else if (object instanceof FieldLike f) {
            delegate.addValue(field, index, f.asField());
        } else if (field != null) {
            delegate.addValue(field, index, field.getDataType().convert(object));
        } else {
            delegate.addValue(field, index, (T) object);
        }
    }

    public SELF values(Field<T1> value1) {
        return values(new Field[]{value1});
    }

    public SELF values(Field<T1> value1, Field<T2> value2) {
        return values(new Field[]{value1, value2});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3) {
        return values(new Field[]{value1, value2, value3});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4) {
        return values(new Field[]{value1, value2, value3, value4});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5) {
        return values(new Field[]{value1, value2, value3, value4, value5});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10, Field<T11> value11) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10, Field<T11> value11, Field<T12> value12) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10, Field<T11> value11, Field<T12> value12, Field<T13> value13) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10, Field<T11> value11, Field<T12> value12, Field<T13> value13, Field<T14> value14) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10, Field<T11> value11, Field<T12> value12, Field<T13> value13, Field<T14> value14, Field<T15> value15) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10, Field<T11> value11, Field<T12> value12, Field<T13> value13, Field<T14> value14, Field<T15> value15, Field<T16> value16) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10, Field<T11> value11, Field<T12> value12, Field<T13> value13, Field<T14> value14, Field<T15> value15, Field<T16> value16, Field<T17> value17) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16, value17});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10, Field<T11> value11, Field<T12> value12, Field<T13> value13, Field<T14> value14, Field<T15> value15, Field<T16> value16, Field<T17> value17, Field<T18> value18) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16, value17, value18});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10, Field<T11> value11, Field<T12> value12, Field<T13> value13, Field<T14> value14, Field<T15> value15, Field<T16> value16, Field<T17> value17, Field<T18> value18, Field<T19> value19) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16, value17, value18, value19});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10, Field<T11> value11, Field<T12> value12, Field<T13> value13, Field<T14> value14, Field<T15> value15, Field<T16> value16, Field<T17> value17, Field<T18> value18, Field<T19> value19, Field<T20> value20) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16, value17, value18, value19, value20});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10, Field<T11> value11, Field<T12> value12, Field<T13> value13, Field<T14> value14, Field<T15> value15, Field<T16> value16, Field<T17> value17, Field<T18> value18, Field<T19> value19, Field<T20> value20, Field<T21> value21) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16, value17, value18, value19, value20, value21});
    }

    public SELF values(Field<T1> value1, Field<T2> value2, Field<T3> value3, Field<T4> value4, Field<T5> value5, Field<T6> value6, Field<T7> value7, Field<T8> value8, Field<T9> value9, Field<T10> value10, Field<T11> value11, Field<T12> value12, Field<T13> value13, Field<T14> value14, Field<T15> value15, Field<T16> value16, Field<T17> value17, Field<T18> value18, Field<T19> value19, Field<T20> value20, Field<T21> value21, Field<T22> value22) {
        return values(new Field[]{value1, value2, value3, value4, value5, value6, value7, value8, value9, value10, value11, value12, value13, value14, value15, value16, value17, value18, value19, value20, value21, value22});
    }

    @SuppressWarnings("RedundantCast")
    public SELF values(Field<?>... values) {
        if (!Tools.isEmpty(fields) && fields.length != values.length) {
            throw new IllegalArgumentException("The number of values must match the number of fields");
        }

        getDelegate().newRecord();

        if (Tools.isEmpty(fields)) {
            for (int i = 0; i < values.length; i++) {
                addValue(getDelegate(), (Field<Void>) null, i, (Field<Void>) values[i]);
            }
        } else {
            for (int i = 0; i < fields.length; i++) {
                addValue(getDelegate(), (Field<Void>) fields[i], i, (Field<Void>) values[i]);
            }
        }

        return (SELF) this;
    }

    public SELF columns(Field field1) {
        return columns(new Field[]{field1});
    }

    public SELF columns(Field field1, Field field2) {
        return columns(new Field[]{field1, field2});
    }

    public SELF columns(Field field1, Field field2, Field field3) {
        return columns(new Field[]{field1, field2, field3});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4) {
        return columns(new Field[]{field1, field2, field3, field4});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5) {
        return columns(new Field[]{field1, field2, field3, field4, field5});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10, Field field11) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10, Field field11, Field field12) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10, Field field11, Field field12, Field field13) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10, Field field11, Field field12, Field field13, Field field14) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10, Field field11, Field field12, Field field13, Field field14, Field field15) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10, Field field11, Field field12, Field field13, Field field14, Field field15, Field field16) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10, Field field11, Field field12, Field field13, Field field14, Field field15, Field field16, Field field17) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10, Field field11, Field field12, Field field13, Field field14, Field field15, Field field16, Field field17, Field field18) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10, Field field11, Field field12, Field field13, Field field14, Field field15, Field field16, Field field17, Field field18, Field field19) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10, Field field11, Field field12, Field field13, Field field14, Field field15, Field field16, Field field17, Field field18, Field field19, Field field20) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10, Field field11, Field field12, Field field13, Field field14, Field field15, Field field16, Field field17, Field field18, Field field19, Field field20, Field field21) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20, field21});
    }

    public SELF columns(Field field1, Field field2, Field field3, Field field4, Field field5, Field field6, Field field7, Field field8, Field field9, Field field10, Field field11, Field field12, Field field13, Field field14, Field field15, Field field16, Field field17, Field field18, Field field19, Field field20, Field field21, Field field22) {
        return columns(new Field[]{field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20, field21, field22});
    }

    public SELF columns(Field<?>... f) {
        this.fields = Tools.isEmpty(f) ? into.fields() : f;
        return (SELF) this;
    }

    public SELF columns(Collection<? extends Field<?>> f) {
        return columns(f.toArray(Tools.EMPTY_FIELD));
    }

    public <T> SELF set(Field<T> field, T value) {
        getDelegate().addValue(field, value);

        return (SELF) this;
    }

    public <T> SELF set(Field<T> field, Field<T> value) {
        getDelegate().addValue(field, value);

        return (SELF) this;
    }

    public <T> SELF set(Field<T> field, Select<? extends Record1<T>> value) {
        return set(field, value.asField());
    }

    public <T> SELF setNull(Field<T> field) {
        return set(field, (T) null);
    }

    public SELF set(Map<?, ?> map) {
        getDelegate().addValues(map);

        return (SELF) this;
    }

    public SELF set(Record record) {
        return set(Tools.mapOfChangedValues(record));
    }

    public SELF set(Record... records) {
        return set(Arrays.asList(records));
    }

    public SELF set(Collection<? extends Record> records) {
        for (Record record : records) {
            set(record).newRecord();
        }

        return (SELF) this;
    }

    public SELF newRecord() {
        getDelegate().newRecord();

        return (SELF) this;
    }
}
