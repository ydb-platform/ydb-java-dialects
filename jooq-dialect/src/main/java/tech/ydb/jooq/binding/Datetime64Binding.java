package tech.ydb.jooq.binding;

import java.sql.SQLException;
import java.time.LocalDateTime;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import org.jooq.impl.IdentityConverter;
import static tech.ydb.jooq.binding.BindingTools.indexType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

public final class Datetime64Binding extends AbstractBinding<LocalDateTime, LocalDateTime> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Datetime64);

    @Override
    public Converter<LocalDateTime, LocalDateTime> converter() {
        return new IdentityConverter<>(LocalDateTime.class);
    }

    @Override
    public void set(BindingSetStatementContext<LocalDateTime> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newDatetime64(ctx.value()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<LocalDateTime> ctx) throws SQLException {
        LocalDateTime value = (LocalDateTime) ctx.resultSet().getObject(ctx.index());
        ctx.value(value);
    }
}
