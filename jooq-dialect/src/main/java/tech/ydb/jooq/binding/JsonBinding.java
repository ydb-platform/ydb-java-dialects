package tech.ydb.jooq.binding;

import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.JSON;
import org.jooq.impl.AbstractBinding;
import org.jooq.impl.IdentityConverter;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

import java.sql.SQLException;

import static tech.ydb.jooq.binding.BindingTools.indexType;

@SuppressWarnings("resource")
public final class JsonBinding extends AbstractBinding<JSON, JSON> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Json);

    @Override
    public Converter<JSON, JSON> converter() {
        return new IdentityConverter<>(JSON.class);
    }

    @Override
    public void set(BindingSetStatementContext<JSON> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newJson(ctx.value().data()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<JSON> ctx) throws SQLException {
        String value = ctx.resultSet().getString(ctx.index());
        ctx.value(JSON.jsonOrNull(value));
    }
}
