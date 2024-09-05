package tech.ydb.jooq.impl;

import io.r2dbc.spi.ConnectionFactory;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.*;
import tech.ydb.jooq.*;
import tech.ydb.jooq.dsl.replace.*;
import tech.ydb.jooq.dsl.upsert.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static java.util.Arrays.asList;

@SuppressWarnings("NullableProblems")
public class YdbDSLContextImpl extends DefaultDSLContext implements YdbDSLContext {
    public YdbDSLContextImpl() {
        this(new DefaultConfiguration());
    }

    public YdbDSLContextImpl(Settings settings) {
        this(new DefaultConfiguration().set(settings));
    }

    public YdbDSLContextImpl(Connection connection) {
        this(new DefaultConfiguration().set(connection));
    }

    public YdbDSLContextImpl(Connection connection, Settings settings) {
        this(new DefaultConfiguration()
                .set(connection)
                .set(settings));
    }

    public YdbDSLContextImpl(DataSource datasource) {
        this(new DefaultConfiguration().set(datasource));
    }

    public YdbDSLContextImpl(DataSource datasource, Settings settings) {
        this(new DefaultConfiguration().set(datasource).set(settings));
    }

    public YdbDSLContextImpl(ConnectionProvider connectionProvider) {
        this(new DefaultConfiguration().set(connectionProvider));
    }

    public YdbDSLContextImpl(ConnectionProvider connectionProvider, Settings settings) {
        this(new DefaultConfiguration().set(connectionProvider).set(settings));
    }

    public YdbDSLContextImpl(ConnectionFactory connectionFactory) {
        this(new DefaultConfiguration().set(connectionFactory));
    }

    public YdbDSLContextImpl(ConnectionFactory connectionFactory, Settings settings) {
        this(new DefaultConfiguration()
                        .set(connectionFactory)
                        .set(settings));
    }

    public YdbDSLContextImpl(Configuration configuration) {
        super(configuration
                .deriveSettings(YdbDSLContextImpl::addRequiredParameters)
                .set(YDB.DIALECT)
                .set(ydbListener()));
    }

    private static Settings addRequiredParameters(Settings settings) {
        return settings
                .withRenderQuotedNames(RenderQuotedNames.NEVER)
                .withRenderSchema(false);
    }

    private static VisitListener ydbListener() {
        return new YdbListener("`");
    }

    @Override
    public <R extends Record> UpsertQuery<R> upsertQuery(Table<R> into) {
        return new UpsertReplaceQueryImpl<>(configuration(), into, YdbKeywords.K_UPSERT);
    }

    @Override
    public <R extends Record> UpsertSetStep<R> upsertInto(Table<R> into) {
        return new UpsertImpl<>(configuration(), into, Collections.emptyList());
    }

    @Override
    public <R extends Record, T1> UpsertValuesStep1<R, T1> upsertInto(Table<R> into, Field<T1> field1) {
        return new UpsertImpl<>(configuration(), into, Collections.singletonList(field1));
    }

    @Override
    public <R extends Record, T1, T2> UpsertValuesStep2<R, T1, T2> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2));
    }

    @Override
    public <R extends Record, T1, T2, T3> UpsertValuesStep3<R, T1, T2, T3> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4> UpsertValuesStep4<R, T1, T2, T3, T4> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5> UpsertValuesStep5<R, T1, T2, T3, T4, T5> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6> UpsertValuesStep6<R, T1, T2, T3, T4, T5, T6> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7> UpsertValuesStep7<R, T1, T2, T3, T4, T5, T6, T7> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8> UpsertValuesStep8<R, T1, T2, T3, T4, T5, T6, T7, T8> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9> UpsertValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> UpsertValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> UpsertValuesStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> UpsertValuesStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> UpsertValuesStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> UpsertValuesStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> UpsertValuesStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> UpsertValuesStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> UpsertValuesStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> UpsertValuesStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> UpsertValuesStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> UpsertValuesStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> UpsertValuesStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20, field21));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> UpsertValuesStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> upsertInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21, Field<T22> field22) {
        return new UpsertImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20, field21, field22));
    }

    @Override
    public <R extends Record> UpsertValuesStepN<R> upsertInto(Table<R> into, Field<?>... fields) {
        return new UpsertImpl<>(configuration(), into, Arrays.asList(fields));
    }

    @Override
    public <R extends Record> UpsertValuesStepN<R> upsertInto(Table<R> into, Collection<? extends Field<?>> fields) {
        return new UpsertImpl<>(configuration(), into, fields);
    }

    @Override
    public <R extends Record> ReplaceQuery<R> replaceQuery(Table<R> into) {
        return new UpsertReplaceQueryImpl<>(configuration(), into, YdbKeywords.K_REPLACE);
    }

    @Override
    public <R extends Record> ReplaceSetStep<R> replaceInto(Table<R> into) {
        return new ReplaceImpl<>(configuration(), into, Collections.emptyList());
    }

    @Override
    public <R extends Record, T1> ReplaceValuesStep1<R, T1> replaceInto(Table<R> into, Field<T1> field1) {
        return new ReplaceImpl<>(configuration(), into, Collections.singletonList(field1));
    }

    @Override
    public <R extends Record, T1, T2> ReplaceValuesStep2<R, T1, T2> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2));
    }

    @Override
    public <R extends Record, T1, T2, T3> ReplaceValuesStep3<R, T1, T2, T3> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4> ReplaceValuesStep4<R, T1, T2, T3, T4> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5> ReplaceValuesStep5<R, T1, T2, T3, T4, T5> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6> ReplaceValuesStep6<R, T1, T2, T3, T4, T5, T6> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7> ReplaceValuesStep7<R, T1, T2, T3, T4, T5, T6, T7> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8> ReplaceValuesStep8<R, T1, T2, T3, T4, T5, T6, T7, T8> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9> ReplaceValuesStep9<R, T1, T2, T3, T4, T5, T6, T7, T8, T9> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> ReplaceValuesStep10<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> ReplaceValuesStep11<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> ReplaceValuesStep12<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> ReplaceValuesStep13<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> ReplaceValuesStep14<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> ReplaceValuesStep15<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> ReplaceValuesStep16<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> ReplaceValuesStep17<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> ReplaceValuesStep18<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> ReplaceValuesStep19<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> ReplaceValuesStep20<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> ReplaceValuesStep21<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20, field21));
    }

    @Override
    public <R extends Record, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> ReplaceValuesStep22<R, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22> replaceInto(Table<R> into, Field<T1> field1, Field<T2> field2, Field<T3> field3, Field<T4> field4, Field<T5> field5, Field<T6> field6, Field<T7> field7, Field<T8> field8, Field<T9> field9, Field<T10> field10, Field<T11> field11, Field<T12> field12, Field<T13> field13, Field<T14> field14, Field<T15> field15, Field<T16> field16, Field<T17> field17, Field<T18> field18, Field<T19> field19, Field<T20> field20, Field<T21> field21, Field<T22> field22) {
        return new ReplaceImpl<>(configuration(), into, asList(field1, field2, field3, field4, field5, field6, field7, field8, field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19, field20, field21, field22));
    }

    @Override
    public <R extends Record> ReplaceValuesStepN<R> replaceInto(Table<R> into, Field<?>... fields) {
        return new ReplaceImpl<>(configuration(), into, Arrays.asList(fields));
    }

    @Override
    public <R extends Record> ReplaceValuesStepN<R> replaceInto(Table<R> into, Collection<? extends Field<?>> fields) {
        return new ReplaceImpl<>(configuration(), into, fields);
    }
}
