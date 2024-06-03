package tech.ydb.jooq.binding;

import org.jetbrains.annotations.NotNull;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.AbstractBinding;
import tech.ydb.jooq.value.YSON;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

import java.sql.SQLException;

import static tech.ydb.jooq.binding.BindingTools.indexType;

public class YsonBinding extends AbstractBinding<byte[], YSON> {

    private static final int INDEX_TYPE = indexType(PrimitiveType.Yson);

    @NotNull
    @Override
    public Converter<byte[], YSON> converter() {
        return new YsonConverter();
    }

    @Override
    public void set(BindingSetStatementContext<YSON> ctx) throws SQLException {
        ctx.statement().setObject(ctx.index(), PrimitiveValue.newYson(ctx.value().data()), INDEX_TYPE);
    }

    @Override
    public void get(BindingGetResultSetContext<YSON> ctx) throws SQLException {
        byte[] value = ctx.resultSet().getBytes(ctx.index());
        ctx.value(YSON.ysonOrNull(value));
    }

    private static class YsonConverter implements Converter<byte[], YSON> {
        @Override
        public YSON from(byte[] databaseObject) {
            return YSON.valueOf(databaseObject);
        }

        @Override
        public byte[] to(YSON userObject) {
            return userObject.data();
        }

        @Override
        public Class<byte[]> fromType() {
            return byte[].class;
        }

        @Override
        public Class<YSON> toType() {
            return YSON.class;
        }
    }
}

