package org.jooq.impl;

import org.jooq.Configuration;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import tech.ydb.jooq.YdbKeywords;
import tech.ydb.jooq.dsl.replace.*;

import java.util.Collection;

@SuppressWarnings({"NullableProblems", "rawtypes"})
public final class ReplaceImpl<R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>
        extends UpsertReplaceImpl<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, ReplaceImpl>
        implements
        // Cascading interface implementations for Replace behaviour
        ReplaceValuesStep1<R, T1>,
        ReplaceValuesStep2<R, T1, T2>,
        ReplaceValuesStep3<R, T1, T2, T3>,
        ReplaceValuesStep4<R, T1, T2, T3, T4>,
        ReplaceValuesStep5<R, T1, T2, T3, T4, T5>,
        ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6>,
        ReplaceValuesStep7<R, T1, T2, T3, T4, T5, T6, T7>,
        ReplaceValuesStep8<R, T1, T2, T3, T4, T5, T6, T7, T8>,
        ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9>,
        ReplaceValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>,
        ReplaceValuesStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>,
        ReplaceValuesStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>,
        ReplaceValuesStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>,
        ReplaceValuesStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>,
        ReplaceValuesStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>,
        ReplaceValuesStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>,
        ReplaceValuesStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>,
        ReplaceValuesStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>,
        ReplaceValuesStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>,
        ReplaceValuesStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>,
        ReplaceValuesStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21>,
        ReplaceValuesStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22>,
        ReplaceValuesStepN<R>,
        ReplaceSetStep<R>,
        ReplaceSetMoreStep<R> {

    public ReplaceImpl(Configuration configuration, Table<R> into) {
        super(configuration, into, YdbKeywords.K_REPLACE);
    }

    public ReplaceImpl(Configuration configuration, Table<R> into, Collection<? extends Field<?>> collection) {
        super(configuration, into, collection, YdbKeywords.K_REPLACE);
    }
}
