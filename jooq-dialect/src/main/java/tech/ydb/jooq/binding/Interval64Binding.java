package tech.ydb.jooq.binding;

import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import org.jooq.types.ULong;
import static tech.ydb.jooq.binding.BindingTools.indexType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

/**
 * @author Kirill Kurdyukov
 */
public class Interval64Binding extends AbstractBinding<ULong, Duration> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Interval64);

    @Override
    public Converter<ULong, Duration> converter() {
        return new IntervalConverter();
    }

    @Override
    public void set(BindingSetStatementContext<Duration> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newInterval64(ctx.value()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<Duration> ctx) throws SQLException {
        Duration value = (Duration) ctx.resultSet().getObject(ctx.index());
        ctx.value(value);
    }

    private static class IntervalConverter implements Converter<ULong, Duration> {

        @Override
        public Duration from(ULong databaseObject) {
            return databaseObject == null ? null : Duration.of(databaseObject.longValue(), ChronoUnit.MICROS);
        }

        @Override
        public ULong to(Duration userObject) {
            return userObject == null ? null : ULong.valueOf(TimeUnit.NANOSECONDS.toMicros(userObject.toNanos()));
        }

        @Override
        public Class<ULong> fromType() {
            return ULong.class;
        }

        @Override
        public Class<Duration> toType() {
            return Duration.class;
        }
    }
}
