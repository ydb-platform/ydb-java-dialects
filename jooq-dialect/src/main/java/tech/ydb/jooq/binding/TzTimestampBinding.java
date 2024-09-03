package tech.ydb.jooq.binding;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import org.jooq.impl.IdentityConverter;
import static tech.ydb.jooq.binding.BindingTools.indexType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

@SuppressWarnings("resource")
public final class TzTimestampBinding extends AbstractBinding<ZonedDateTime, ZonedDateTime> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.TzTimestamp);

    @Override
    public Converter<ZonedDateTime, ZonedDateTime> converter() {
        return new IdentityConverter<>(ZonedDateTime.class);
    }

    @Override
    public void set(BindingSetStatementContext<ZonedDateTime> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newTzTimestamp(ctx.value()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<ZonedDateTime> ctx) throws SQLException {
        ZonedDateTime value = (ZonedDateTime) ctx.resultSet().getObject(ctx.index());
        ctx.value(value);
    }
}

