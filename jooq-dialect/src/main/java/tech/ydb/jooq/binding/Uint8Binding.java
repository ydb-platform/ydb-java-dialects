package tech.ydb.jooq.binding;

import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import org.jooq.impl.IdentityConverter;
import org.jooq.types.UByte;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

import java.sql.SQLException;

import static tech.ydb.jooq.binding.BindingTools.indexType;

@SuppressWarnings("resource")
public final class Uint8Binding extends AbstractBinding<UByte, UByte> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Uint8);

    @Override
    public Converter<UByte, UByte> converter() {
        return new IdentityConverter<>(UByte.class);
    }

    @Override
    public void set(BindingSetStatementContext<UByte> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newUint8(ctx.value().byteValue()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<UByte> ctx) throws SQLException {
        short value = ctx.resultSet().getShort(ctx.index());
        ctx.value(UByte.valueOf(value));
    }
}
