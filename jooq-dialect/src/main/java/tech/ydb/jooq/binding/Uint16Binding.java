package tech.ydb.jooq.binding;

import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import org.jooq.impl.IdentityConverter;
import org.jooq.types.UShort;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

import java.sql.SQLException;

import static tech.ydb.jooq.binding.BindingTools.indexType;

public final class Uint16Binding extends AbstractBinding<UShort, UShort> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Uint16);

    @Override
    public Converter<UShort, UShort> converter() {
        return new IdentityConverter<>(UShort.class);
    }

    @Override
    public void set(BindingSetStatementContext<UShort> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newUint16(ctx.value().intValue()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<UShort> ctx) throws SQLException {
        int value = ctx.resultSet().getInt(ctx.index());
        ctx.value(UShort.valueOf(value));
    }
}
