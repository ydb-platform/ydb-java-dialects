package tech.ydb.jooq.binding;

import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import org.jooq.impl.IdentityConverter;
import org.jooq.types.ULong;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

import java.sql.SQLException;

import static tech.ydb.jooq.binding.BindingTools.indexType;

public final class Uint64Binding extends AbstractBinding<ULong, ULong> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Uint64);

    @Override
    public Converter<ULong, ULong> converter() {
        return new IdentityConverter<>(ULong.class);
    }

    @Override
    public void set(BindingSetStatementContext<ULong> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newUint64(ctx.value().longValue()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<ULong> ctx) throws SQLException {
        long value = ctx.resultSet().getLong(ctx.index());
        ctx.value(ULong.valueOf(value));
    }
}
