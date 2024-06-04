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
import java.time.LocalDate;

import static tech.ydb.jooq.binding.BindingTools.indexType;

@SuppressWarnings("resource")
public final class DateBinding extends AbstractBinding<LocalDate, LocalDate> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Date);

    @NotNull
    @Override
    public Converter<LocalDate, LocalDate> converter() {
        return new IdentityConverter<>(LocalDate.class);
    }

    @Override
    public void set(BindingSetStatementContext<LocalDate> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newDate(ctx.value()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<LocalDate> ctx) throws SQLException {
        LocalDate value = (LocalDate) ctx.resultSet().getObject(ctx.index());
        ctx.value(value);
    }
}
