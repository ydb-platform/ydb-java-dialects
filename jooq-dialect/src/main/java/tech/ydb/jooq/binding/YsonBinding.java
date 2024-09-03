package tech.ydb.jooq.binding;

import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import tech.ydb.jooq.value.YSON;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

import java.sql.SQLException;

import static tech.ydb.jooq.binding.BindingTools.indexType;

@SuppressWarnings("resource")
public final class YsonBinding extends AbstractBinding<Object, YSON> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Yson);

    @Override
    public Converter<Object, YSON> converter() {
        return new YsonConverter();
    }

    @Override
    public void set(BindingSetStatementContext<YSON> ctx) throws SQLException {
        if (ctx.value() == null) {
            ctx.statement().setNull(ctx.index(), INDEX_TYPE);
        } else {
            ctx.statement().setObject(ctx.index(), PrimitiveValue.newYson(ctx.value().data()), INDEX_TYPE);
        }
    }

    @Override
    public void get(BindingGetResultSetContext<YSON> ctx) throws SQLException {
        byte[] value = ctx.resultSet().getBytes(ctx.index());
        ctx.value(YSON.ysonOrNull(value));
    }

    private static class YsonConverter implements Converter<Object, YSON> {
        @Override
        public YSON from(Object databaseObject) {
            return (YSON) databaseObject;
        }

        @Override
        public Object to(YSON userObject) {
            return userObject;
        }


        @Override
        public Class<Object> fromType() {
            return Object.class;
        }

        @Override
        public Class<YSON> toType() {
            return YSON.class;
        }
    }
}

