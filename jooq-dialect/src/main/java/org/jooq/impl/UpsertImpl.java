package org.jooq.impl;

import org.jooq.Configuration;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import tech.ydb.jooq.YdbKeywords;
import tech.ydb.jooq.dsl.upsert.*;

import java.util.Collection;

@SuppressWarnings({"NullableProblems", "rawtypes"})
public final class UpsertImpl<R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>
        extends UpsertReplaceImpl<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, UpsertImpl>
        implements
        // Cascading interface implementations for Upsert behaviour
        UpsertValuesStep1<R, T1>,
        UpsertValuesStep2<R, T1, T2>,
        UpsertValuesStep3<R, T1, T2, T3>,
        UpsertValuesStep4<R, T1, T2, T3, T4>,
        UpsertValuesStep5<R, T1, T2, T3, T4, T5>,
        UpsertValuesStep6<R, T1, T2, T3, T4, T5, T6>,
        UpsertValuesStep7<R, T1, T2, T3, T4, T5, T6, T7>,
        UpsertValuesStep8<R, T1, T2, T3, T4, T5, T6, T7, T8>,
        UpsertValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9>,
        UpsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>,
        UpsertValuesStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>,
        UpsertValuesStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>,
        UpsertValuesStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>,
        UpsertValuesStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>,
        UpsertValuesStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>,
        UpsertValuesStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>,
        UpsertValuesStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>,
        UpsertValuesStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>,
        UpsertValuesStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>,
        UpsertValuesStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>,
        UpsertValuesStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21>,
        UpsertValuesStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>,
        UpsertValuesStepN<R>,
        UpsertSetStep<R>,
        UpsertSetMoreStep<R> {

    public UpsertImpl(Configuration configuration, Table<R> into) {
        super(configuration, into, YdbKeywords.K_UPSERT);
    }

    public UpsertImpl(Configuration configuration, Table<R> into, Collection<? extends Field<?>> collection) {
        super(configuration, into, collection, YdbKeywords.K_UPSERT);
    }
}
