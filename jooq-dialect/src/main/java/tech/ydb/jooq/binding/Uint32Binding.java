package tech.ydb.jooq.binding;

import org.jetbrains.annotations.NotNull;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import org.jooq.impl.IdentityConverter;
import org.jooq.types.UInteger;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

import java.sql.SQLException;

import static tech.ydb.jooq.binding.BindingTools.indexType;

@SuppressWarnings("resource")
public final class Uint32Binding extends AbstractBinding<UInteger, UInteger> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Uint32);

    @NotNull
    @Override
    public Converter<UInteger, UInteger> converter() {
        return new IdentityConverter<>(UInteger.class);
    }

    @Override
    public void set(BindingSetStatementContext<UInteger> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newUint32(ctx.value().longValue()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<UInteger> ctx) throws SQLException {
        long value = ctx.resultSet().getLong(ctx.index());
        ctx.value(UInteger.valueOf(value));
    }
}
