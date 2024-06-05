package tech.ydb.jooq.binding;

import org.jetbrains.annotations.NotNull;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import org.jooq.impl.IdentityConverter;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static tech.ydb.jooq.binding.BindingTools.indexType;

@SuppressWarnings("resource")
public final class DatetimeBinding extends AbstractBinding<LocalDateTime, LocalDateTime> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Datetime);

    @NotNull
    @Override
    public Converter<LocalDateTime, LocalDateTime> converter() {
        return new IdentityConverter<>(LocalDateTime.class);
    }

    @Override
    public void set(BindingSetStatementContext<LocalDateTime> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newDatetime(ctx.value()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<LocalDateTime> ctx) throws SQLException {
        LocalDateTime value = (LocalDateTime) ctx.resultSet().getObject(ctx.index());
        ctx.value(value);
    }
}
