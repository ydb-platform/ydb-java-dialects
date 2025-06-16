package tech.ydb.jooq.binding;

import java.sql.SQLException;
import java.time.Duration;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import org.jooq.types.YearToSecond;
import static tech.ydb.jooq.binding.BindingTools.indexType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

public final class IntervalBinding extends AbstractBinding<YearToSecond, Duration> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Interval);


    @Override
    public Converter<YearToSecond, Duration> converter() {
        return new IntervalConverter();
    }

    @Override
    public void set(BindingSetStatementContext<Duration> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newInterval(ctx.value()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<Duration> ctx) throws SQLException {
        Duration value = (Duration) ctx.resultSet().getObject(ctx.index());
        ctx.value(value);
    }

    private static class IntervalConverter implements Converter<YearToSecond, Duration> {
        @Override
        public Duration from(YearToSecond databaseObject) {
            return databaseObject == null ? null : databaseObject.toDuration();
        }

        @Override
        public YearToSecond to(Duration userObject) {
            return YearToSecond.valueOf(userObject);
        }

        @Override
        public Class<YearToSecond> fromType() {
            return YearToSecond.class;
        }

        @Override
        public Class<Duration> toType() {
            return Duration.class;
        }
    }
}

