package tech.ydb.jooq.binding;

import java.sql.SQLException;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.JSONB;
import org.jooq.impl.AbstractBinding;
import org.jooq.impl.IdentityConverter;
import static tech.ydb.jooq.binding.BindingTools.indexType;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

public final class JsonDocumentBinding extends AbstractBinding<JSONB, JSONB> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.JsonDocument);

    @Override
    public Converter<JSONB, JSONB> converter() {
        return new IdentityConverter<>(JSONB.class);
    }

    @Override
    public void set(BindingSetStatementContext<JSONB> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newJsonDocument(ctx.value().data()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<JSONB> ctx) throws SQLException {
        byte[] value = ctx.resultSet().getBytes(ctx.index());
        ctx.value(JSONB.jsonbOrNull(value != null ? new String(value) : null));
    }
}

