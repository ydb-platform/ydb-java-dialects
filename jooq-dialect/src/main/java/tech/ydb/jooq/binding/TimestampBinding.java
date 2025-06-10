package tech.ydb.jooq.binding;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import static tech.ydb.jooq.binding.BindingTools.indexType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

public final class TimestampBinding extends AbstractBinding<LocalDateTime, Instant> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Timestamp);

    @Override
    public Converter<LocalDateTime, Instant> converter() {
        return new TimestampConverter();
    }

    @Override
    public void set(BindingSetStatementContext<Instant> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newTimestamp(ctx.value()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<Instant> ctx) throws SQLException {
        Instant value = (Instant) ctx.resultSet().getObject(ctx.index());
        ctx.value(value);
    }

    private static class TimestampConverter implements Converter<LocalDateTime, Instant> {
        @Override
        public Instant from(LocalDateTime databaseObject) {
            if (databaseObject == null) {
                return null;
            }
            return databaseObject.toInstant(ZoneOffset.UTC);
        }

        @Override
        public LocalDateTime to(Instant userObject) {
            return LocalDateTime.ofInstant(userObject, ZoneOffset.UTC);
        }

        @Override
        public Class<LocalDateTime> fromType() {
            return LocalDateTime.class;
        }

        @Override
        public Class<Instant> toType() {
            return Instant.class;
        }
    }
}

